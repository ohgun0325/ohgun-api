package site.ohgun.api.oauth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.ohgun.api.oauth.jwt.JwtProperties;
import site.ohgun.api.oauth.jwt.JwtTokenProvider;
import site.ohgun.api.oauth.redis.RefreshTokenService;
import site.ohgun.api.user.entity.User;
import site.ohgun.api.user.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 공통 인증 기능을 처리하는 Controller
 *
 * 역할:
 * - Refresh Token으로 Access Token 재발급 (회전 포함)
 * - 토큰 검증
 * - 로그아웃 (서버 측 처리)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    /**
     * Refresh Token으로 Access Token 재발급
     * POST /api/auth/refresh
     * 
     * Refresh Token 회전(Rotation) 적용:
     * - 이전 Refresh Token을 블랙리스트에 추가
     * - 새 Access Token과 새 Refresh Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            // 1. Refresh Token 추출 (쿠키 또는 요청 본문)
            String refreshToken = refreshTokenCookie;
            if (refreshToken == null || refreshToken.isEmpty()) {
                // 쿠키가 없으면 요청 본문에서 확인
                @SuppressWarnings("unchecked")
                Map<String, String> body = request.getAttribute("requestBody") != null 
                        ? (Map<String, String>) request.getAttribute("requestBody") 
                        : new HashMap<>();
                refreshToken = body.get("refreshToken");
            }

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token이 필요합니다."));
            }

            // 2. Refresh Token 유효성 검증 (Redis)
            if (!refreshTokenService.isValidRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "유효하지 않은 Refresh Token입니다."));
            }

            // 3. Refresh Token에서 사용자 ID 추출
            String userId = refreshTokenService.getUserIdByRefreshToken(refreshToken);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh Token을 찾을 수 없습니다."));
            }

            // 4. 사용자 정보 조회
            Optional<User> userOpt = userRepository.findById(Long.parseLong(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "사용자를 찾을 수 없습니다."));
            }
            User user = userOpt.get();

            // 5. 새 Access Token 발급
            String newAccessToken = jwtTokenProvider.createAccessToken(
                    user.getId().toString(),
                    Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail() != null ? user.getEmail() : "",
                            "name", user.getName() != null ? user.getName() : "",
                            "role", user.getRole()
                    )
            );

            // 6. 새 Refresh Token 발급 (회전)
            String newRefreshToken = jwtTokenProvider.createRefreshToken(
                    user.getId().toString(),
                    Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail() != null ? user.getEmail() : "",
                            "role", user.getRole()
                    )
            );

            // 7. Refresh Token 회전 (이전 토큰 블랙리스트, 새 토큰 저장)
            refreshTokenService.rotateRefreshToken(
                    userId,
                    refreshToken,
                    newRefreshToken,
                    jwtProperties.getRefreshTokenValidityInSeconds()
            );

            // 8. 새 Refresh Token을 HttpOnly 쿠키에 설정
            Cookie cookie = new Cookie("refreshToken", newRefreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true); // HTTPS에서만 전송
            cookie.setPath("/");
            cookie.setMaxAge((int) jwtProperties.getRefreshTokenValidityInSeconds());
            response.addCookie(cookie);

            log.debug("Token refreshed for user: {}", userId);

            // 9. 새 Access Token 반환
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "message", "토큰이 재발급되었습니다."
            ));

        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "토큰 재발급 중 오류가 발생했습니다."));
        }
    }

    /**
     * 토큰 검증
     * POST /api/auth/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "토큰이 필요합니다."));
            }

            // JWT 토큰 파싱 및 검증
            Claims claims = jwtTokenProvider.parseToken(token);
            
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "userId", claims.get("userId"),
                    "email", claims.get("email"),
                    "role", claims.get("role"),
                    "exp", claims.getExpiration().getTime()
            ));
        } catch (Exception e) {
            log.debug("Token verification failed", e);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", "유효하지 않은 토큰입니다."
            ));
        }
    }

    /**
     * 로그아웃 (서버 측 처리)
     * POST /api/auth/logout
     * 
     * - Refresh Token을 Redis에서 삭제
     * - Refresh Token을 블랙리스트에 추가
     * - HttpOnly 쿠키 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            String refreshToken = refreshTokenCookie;
            
            if (refreshToken != null && !refreshToken.isEmpty()) {
                // Refresh Token 삭제 및 블랙리스트 추가
                String userId = refreshTokenService.getUserIdByRefreshToken(refreshToken);
                if (userId != null) {
                    refreshTokenService.deleteRefreshToken(refreshToken);
                    refreshTokenService.blacklistToken(refreshToken, jwtProperties.getRefreshTokenValidityInSeconds());
                    log.info("User logged out: {}", userId);
                }
            }

            // HttpOnly 쿠키 삭제
            Cookie cookie = new Cookie("refreshToken", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "로그아웃 중 오류가 발생했습니다."));
        }
    }
}

