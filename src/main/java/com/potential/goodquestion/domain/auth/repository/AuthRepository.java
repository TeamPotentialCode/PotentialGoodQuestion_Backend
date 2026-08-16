package com.potential.goodquestion.domain.auth.repository;

import com.potential.goodquestion.domain.auth.entity.Auth;
import com.potential.goodquestion.domain.parent.entity.Parent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 보호자의 리프레시 토큰 삭제 (회원 탈퇴 시 정리용)
     */
    @Modifying
    @Query("DELETE FROM Auth a WHERE a.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);
}
