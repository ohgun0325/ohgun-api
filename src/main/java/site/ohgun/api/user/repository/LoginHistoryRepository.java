package site.ohgun.api.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.ohgun.api.user.entity.LoginHistory;
import site.ohgun.api.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 로그인 이력 Repository (Neon PostgreSQL)
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    /**
     * 사용자의 로그인 이력 조회 (최신순)
     */
    List<LoginHistory> findByUserOrderByLoginAtDesc(User user);

    /**
     * 사용자의 최근 N개 로그인 이력 조회
     */
    List<LoginHistory> findTop10ByUserOrderByLoginAtDesc(User user);

    /**
     * 특정 기간 동안의 로그인 이력 조회
     */
    List<LoginHistory> findByUserAndLoginAtBetween(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 실패한 로그인 시도 조회
     */
    List<LoginHistory> findByUserAndSuccessFalseOrderByLoginAtDesc(User user);
}

