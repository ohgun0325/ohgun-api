package site.ohgun.api.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import site.ohgun.api.oauth.jwt.JwtTokenProvider;

/**
 * 공통 인증 기능을 처리하는 Controller
 *
 * 역할:
 * - Refresh Token으로 Access Token 재발급
 * - 토큰 검증
 * - 로그아웃 (서버 측 처리)
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token으로 Access Token 재발급
     * POST /api/auth/refresh
     */
    // TODO: Refresh Token으로 Access Token 재발급
    // @PostMapping("/api/auth/refresh")
    // public ResponseEntity<?> refreshToken(...) { }

    /**
     * 토큰 검증
     * POST /api/auth/verify
     */
    // TODO: 토큰 검증
    // @PostMapping("/api/auth/verify")
    // public ResponseEntity<?> verifyToken(...) { }

    /**
     * 로그아웃 (서버 측 처리)
     * POST /api/auth/logout
     */
    // TODO: 로그아웃 (서버 측 처리)
    // @PostMapping("/api/auth/logout")
    // public ResponseEntity<?> logout(...) { }
}

