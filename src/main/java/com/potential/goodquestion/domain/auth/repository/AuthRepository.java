package com.potential.goodquestion.domain.auth.repository;

import com.potential.goodquestion.domain.auth.entity.Auth;
import com.potential.goodquestion.domain.parent.entity.Parent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Auth 레포지토리
 */
public interface AuthRepository extends JpaRepository<Auth, Long> {

    /**
     * 보호자로 토큰 조회
     */
    Optional<Auth> findByParent(Parent parent);

    /**
     * Refresh Token으로 토큰 조회
     */
    Optional<Auth> findByRefreshToken(String refreshToken);
}
