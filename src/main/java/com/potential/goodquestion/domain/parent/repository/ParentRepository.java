package com.potential.goodquestion.domain.parent.repository;

import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Parent 레포지토리
 */
public interface ParentRepository extends JpaRepository<Parent, Long> {

    /**
     * 소셜 로그인 제공자 + 제공자 ID로 보호자 조회
     */
    Optional<Parent> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    /**
     * 이메일로 보호자 조회
     */
    Optional<Parent> findByEmail(String email);

    /**
     * 특정 기간 내 가입한 보호자 수 (오늘 신규 가입 수 집계용)
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
