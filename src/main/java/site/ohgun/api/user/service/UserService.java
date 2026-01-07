package site.ohgun.api.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.ohgun.api.user.entity.LoginHistory;
import site.ohgun.api.user.entity.User;
import site.ohgun.api.user.repository.LoginHistoryRepository;
import site.ohgun.api.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 사용자 서비스 (Neon PostgreSQL)
 * 
 * 역할:
 * - 사용자 생성/조회/업데이트
 * - 로그인 이력 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * OAuth 제공자와 제공자 ID로 사용자 찾기 또는 생성
     * 
     * @param oauthProvider - OAuth 제공자 (naver, kakao 등)
     * @param oauthProviderId - OAuth 제공자에서의 사용자 ID
     * @param email - 이메일
     * @param name - 이름
     * @param nickname - 닉네임
     * @param profileImageUrl - 프로필 이미지 URL
     * @return 사용자 엔티티
     */
    @Transactional
    public User findOrCreateUser(
            String oauthProvider,
            String oauthProviderId,
            String email,
            String name,
            String nickname,
            String profileImageUrl
    ) {
        // 기존 사용자 찾기
        Optional<User> existingUser = userRepository.findByOauthProviderAndOauthProviderId(
                oauthProvider, oauthProviderId
        );

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // 정보 업데이트 (이름, 닉네임, 프로필 이미지 등이 변경되었을 수 있음)
            user.setName(name);
            user.setNickname(nickname);
            user.setProfileImageUrl(profileImageUrl);
            user.setLastLoginAt(LocalDateTime.now());
            
            return userRepository.save(user);
        } else {
            // 새 사용자 생성
            User newUser = User.builder()
                    .oauthProvider(oauthProvider)
                    .oauthProviderId(oauthProviderId)
                    .email(email)
                    .name(name)
                    .nickname(nickname)
                    .profileImageUrl(profileImageUrl)
                    .role("ROLE_USER")
                    .enabled(true)
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            log.info("New user created: {} ({})", email, oauthProvider);
            return userRepository.save(newUser);
        }
    }

    /**
     * 사용자 ID로 조회
     */
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 로그인 이력 저장
     * 
     * @param user - 사용자
     * @param oauthProvider - OAuth 제공자
     * @param success - 성공 여부
     * @param failureReason - 실패 사유 (실패한 경우)
     * @param ipAddress - IP 주소
     * @param userAgent - User-Agent
     */
    @Transactional
    public void saveLoginHistory(
            User user,
            String oauthProvider,
            boolean success,
            String failureReason,
            String ipAddress,
            String userAgent
    ) {
        LoginHistory loginHistory = LoginHistory.builder()
                .user(user)
                .oauthProvider(oauthProvider)
                .success(success)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        loginHistoryRepository.save(loginHistory);
        log.debug("Login history saved for user: {} (success: {})", user.getEmail(), success);
    }
}

