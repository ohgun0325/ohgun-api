package site.ohgun.api.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티 (Neon PostgreSQL)
 * 
 * 역할:
 * - 유저 정보 저장
 * - 권한 정보 저장
 * - OAuth 제공자 정보 저장
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_oauth_provider_id", columnList = "oauth_provider, oauth_provider_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OAuth 제공자 (naver, kakao, google 등)
     */
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private String oauthProvider;

    /**
     * OAuth 제공자에서의 사용자 ID
     */
    @Column(name = "oauth_provider_id", nullable = false, length = 100)
    private String oauthProviderId;

    /**
     * 이메일
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * 이름
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 닉네임
     */
    @Column(name = "nickname", length = 100)
    private String nickname;

    /**
     * 프로필 이미지 URL
     */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * 권한 (ROLE_USER, ROLE_ADMIN 등)
     */
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "ROLE_USER";

    /**
     * 계정 활성화 여부
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * 마지막 로그인 시간
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

