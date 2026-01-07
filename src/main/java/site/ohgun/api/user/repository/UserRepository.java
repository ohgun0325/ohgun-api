package site.ohgun.api.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.ohgun.api.user.entity.User;

import java.util.Optional;

/**
 * 사용자 Repository (Neon PostgreSQL)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * OAuth 제공자와 제공자 ID로 사용자 찾기
     */
    Optional<User> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    /**
     * 이메일로 사용자 찾기
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
}

