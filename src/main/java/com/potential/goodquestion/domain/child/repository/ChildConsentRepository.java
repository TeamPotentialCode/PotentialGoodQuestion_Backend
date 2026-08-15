package com.potential.goodquestion.domain.child.repository;

import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.entity.ChildConsent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

/**
 * 아동 개인정보 처리 동의 레포지토리
 */
public interface ChildConsentRepository extends JpaRepository<ChildConsent, Long> {

    /**
     * 특정 아이의 모든 동의 기록 조회
     */
    List<ChildConsent> findAllByChild(Child child);

    /**
     * 특정 아이의 유효한 동의 기록 조회 (철회되지 않은 최신 동의)
     * withdrawn_at IS NULL 조건으로 유효한 동의만 조회
     */
    @Query("SELECT c FROM ChildConsent c WHERE c.child = :child AND c.withdrawnAt IS NULL ORDER BY c.consentedAt DESC")
    Optional<ChildConsent> findActiveConsentByChild(@Param("child") Child child);

    /**
     * 특정 아이의 유효한 동의 존재 여부 확인
     * 세션 시작 전 동의 여부 검증에 사용
     */
    @Query("SELECT COUNT(c) > 0 FROM ChildConsent c WHERE c.child = :child AND c.withdrawnAt IS NULL")
    boolean existsActiveConsentByChild(@Param("child") Child child);

    /**
     * 보호자의 모든 동의 기록 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM ChildConsent c WHERE c.child.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);
}
