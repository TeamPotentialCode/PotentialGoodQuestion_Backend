package com.potential.goodquestion.domain.session.repository;

import com.potential.goodquestion.domain.session.entity.StorySession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * StorySession 레포지토리
 *
 * 주요 쿼리:
 * - findByIdAndStatus: 전우선 UtteranceService에서 진행 중인 세션 조회 시 사용
 * - findByChildIdOrderByLastActivityAtDesc: 아이의 세션 이력 조회
 */
public interface StorySessionRepository extends JpaRepository<StorySession, Long> {

    /**
     * 세션 ID + 상태로 조회 (전우선 UtteranceService 사용)
     * status 값: "IN_PROGRESS" 또는 "COMPLETED"
     */
    Optional<StorySession> findByIdAndStatus(Long id, String status);

    /**
     * 아이의 세션 목록 조회 (최근 활동 순)
     */
    List<StorySession> findByChildIdOrderByLastActivityAtDesc(Long childId);

    /**
     * 특정 아이 + 이야기 + 상태로 조회 (이어하기 복귀 판단용)
     */
    Optional<StorySession> findByChildIdAndStoryIdAndStatus(Long childId, Long storyId, String status);
}
