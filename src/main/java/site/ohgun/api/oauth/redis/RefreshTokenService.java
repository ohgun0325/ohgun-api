package site.ohgun.api.oauth.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스 (Upstash Redis)
 * 
 * 역할:
 * - Refresh Token 저장/조회/삭제
 * - Refresh Token 회전 (Rotation)
 * - Refresh Token 블랙리스트 관리
 * - 강제 로그아웃 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis Key Prefix
     */
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    /**
     * Refresh Token 저장
     * 
     * @param userId - 사용자 ID
     * @param refreshToken - Refresh Token
     * @param expirationSeconds - 만료 시간 (초)
     */
    public void saveRefreshToken(String userId, String refreshToken, long expirationSeconds) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userTokensKey = USER_TOKENS_PREFIX + userId;

        // Refresh Token 저장 (토큰 자체를 키로 사용)
        redisTemplate.opsForValue().set(tokenKey, userId, expirationSeconds, TimeUnit.SECONDS);

        // 사용자별 토큰 목록에 추가 (회전 시 이전 토큰 찾기 위해)
        redisTemplate.opsForSet().add(userTokensKey, refreshToken);
        redisTemplate.expire(userTokensKey, expirationSeconds, TimeUnit.SECONDS);

        log.debug("Refresh token saved for user: {}", userId);
    }

    /**
     * Refresh Token으로 사용자 ID 조회
     * 
     * @param refreshToken - Refresh Token
     * @return 사용자 ID (없으면 null)
     */
    public String getUserIdByRefreshToken(String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        return redisTemplate.opsForValue().get(tokenKey);
    }

    /**
     * Refresh Token 유효성 검증
     * 
     * @param refreshToken - Refresh Token
     * @return 유효하면 true, 아니면 false
     */
    public boolean isValidRefreshToken(String refreshToken) {
        // 블랙리스트 확인
        if (isBlacklisted(refreshToken)) {
            return false;
        }

        // 토큰 존재 확인
        String userId = getUserIdByRefreshToken(refreshToken);
        return userId != null;
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     * 
     * @param refreshToken - Refresh Token
     */
    public void deleteRefreshToken(String refreshToken) {
        String userId = getUserIdByRefreshToken(refreshToken);
        
        if (userId != null) {
            String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
            String userTokensKey = USER_TOKENS_PREFIX + userId;

            // 토큰 삭제
            redisTemplate.delete(tokenKey);
            
            // 사용자별 토큰 목록에서 제거
            redisTemplate.opsForSet().remove(userTokensKey, refreshToken);

            log.debug("Refresh token deleted for user: {}", userId);
        }
    }

    /**
     * 사용자의 모든 Refresh Token 삭제 (강제 로그아웃)
     * 
     * @param userId - 사용자 ID
     */
    public void deleteAllUserTokens(String userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        
        // 사용자의 모든 토큰 조회
        redisTemplate.opsForSet().members(userTokensKey).forEach(token -> {
            String tokenKey = REFRESH_TOKEN_PREFIX + token;
            redisTemplate.delete(tokenKey);
        });

        // 사용자별 토큰 목록 삭제
        redisTemplate.delete(userTokensKey);

        log.info("All refresh tokens deleted for user: {}", userId);
    }

    /**
     * Refresh Token 회전 (Rotation)
     * 이전 토큰을 블랙리스트에 추가하고 새 토큰 저장
     * 
     * @param userId - 사용자 ID
     * @param oldRefreshToken - 이전 Refresh Token
     * @param newRefreshToken - 새 Refresh Token
     * @param expirationSeconds - 만료 시간 (초)
     */
    public void rotateRefreshToken(String userId, String oldRefreshToken, String newRefreshToken, long expirationSeconds) {
        // 이전 토큰을 블랙리스트에 추가
        blacklistToken(oldRefreshToken, expirationSeconds);

        // 새 토큰 저장
        saveRefreshToken(userId, newRefreshToken, expirationSeconds);

        log.debug("Refresh token rotated for user: {}", userId);
    }

    /**
     * Refresh Token을 블랙리스트에 추가
     * 
     * @param refreshToken - 블랙리스트에 추가할 토큰
     * @param expirationSeconds - 블랙리스트 유지 시간 (초)
     */
    public void blacklistToken(String refreshToken, long expirationSeconds) {
        String blacklistKey = BLACKLIST_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(blacklistKey, "blacklisted", expirationSeconds, TimeUnit.SECONDS);
        log.debug("Refresh token blacklisted: {}", refreshToken);
    }

    /**
     * Refresh Token이 블랙리스트에 있는지 확인
     * 
     * @param refreshToken - 확인할 토큰
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String refreshToken) {
        String blacklistKey = BLACKLIST_PREFIX + refreshToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
}

