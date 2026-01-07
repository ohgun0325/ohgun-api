package site.ohgun.api.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 로그인 이력 엔티티 (Neon PostgreSQL)
 * 
 * 역할:
 * - 로그인 시도 기록
 * - 로그인 성공/실패 기록
 * - 보안 감사(Audit) 용도
 */
@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_login_at", columnList = "login_at"),
    @Index(name = "idx_success", columnList = "success")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 ID (외래키)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OAuth 제공자
     */
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private String oauthProvider;

    /**
     * 로그인 성공 여부
     */
    @Column(name = "success", nullable = false)
    private Boolean success;

    /**
     * 실패 사유 (실패한 경우)
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * IP 주소
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 로그인 시간
     */
    @CreationTimestamp
    @Column(name = "login_at", nullable = false, updatable = false)
    private LocalDateTime loginAt;
}

