package site.ohgun.api.oauth.naver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import site.ohgun.api.oauth.jwt.JwtProperties;
import site.ohgun.api.oauth.jwt.JwtTokenProvider;
import site.ohgun.api.oauth.naver.dto.NaverTokenResponse;
import site.ohgun.api.oauth.naver.dto.NaverUserInfo;
import site.ohgun.api.oauth.naver.dto.OAuthLoginResponse;
import site.ohgun.api.oauth.redis.RefreshTokenService;
import site.ohgun.api.user.entity.User;
import site.ohgun.api.user.service.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/oauth/naver")
@RequiredArgsConstructor
@Slf4j
public class NaverController {

    private final NaverService naverService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @Value("${oauth.frontend.redirect-url:http://localhost:3000}")
    private String frontendRedirectUrl;

    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        String state = UUID.randomUUID().toString();
        String url = naverService.buildAuthorizeUrl(state);
        // TODO: state? Redis? ???? CSRF ?? ??
        return ResponseEntity.ok(Map.of("url", url, "state", state));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request
    ) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            // 1. 네이버에서 토큰 교환
            NaverTokenResponse tokenResponse = naverService.exchangeToken(code, state);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new RuntimeException("네이버 토큰 교환 실패");
            }

            // 2. 네이버에서 사용자 정보 조회
            NaverUserInfo naverUserInfo = naverService.fetchUserInfo(tokenResponse.getAccessToken());
            if (naverUserInfo == null || naverUserInfo.getId() == null) {
                throw new RuntimeException("네이버 사용자 정보 조회 실패");
            }

            // 3. 사용자 저장 또는 업데이트 (Neon PostgreSQL)
            User user = userService.findOrCreateUser(
                    "naver",
                    naverUserInfo.getId(),
                    naverUserInfo.getEmail(),
                    naverUserInfo.getName(),
                    naverUserInfo.getNickname(),
                    naverUserInfo.getProfileImage()
            );

            // 4. JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId().toString(),
                    Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail() != null ? user.getEmail() : "",
                            "name", user.getName() != null ? user.getName() : "",
                            "role", user.getRole()
                    )
            );
            String refreshToken = jwtTokenProvider.createRefreshToken(
                    user.getId().toString(),
                    Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail() != null ? user.getEmail() : "",
                            "role", user.getRole()
                    )
            );

            // 5. Refresh Token을 Upstash Redis에 저장
            refreshTokenService.saveRefreshToken(
                    user.getId().toString(),
                    refreshToken,
                    jwtProperties.getRefreshTokenValidityInSeconds()
            );

            // 6. 로그인 이력 저장 (Neon PostgreSQL)
            userService.saveLoginHistory(
                    user,
                    "naver",
                    true,
                    null,
                    ipAddress,
                    userAgent
            );

            log.info("Login success - User: {} ({}), Email: {}",
                    user.getName(), user.getOauthProvider(), user.getEmail());

            // 7. 프론트엔드로 리다이렉트
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/oauth/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("provider", "naver")
                    .queryParam("success", "true")
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            log.error("OAuth callback error", e);

            // 로그인 실패 이력 저장 (사용자 정보를 알 수 없는 경우는 저장하지 않음)
            // TODO: 실패 이력 저장 로직 추가 가능

            // 에러 페이지로 리다이렉트
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/oauth/error")
                    .queryParam("error", e.getMessage() != null ? e.getMessage() : "로그인 처리 중 오류가 발생했습니다.")
                    .queryParam("provider", "naver")
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorRedirectUrl)
                    .build();
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
