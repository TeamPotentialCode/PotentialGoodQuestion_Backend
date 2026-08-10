package com.potential.goodquestion.domain.storysession.repository;

import com.potential.goodquestion.domain.storysession.entity.StorySession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * StorySession 레포지토리
 */
public interface StorySessionRepository extends JpaRepository<StorySession, Long> {

    /**
     * 세션 ID와 상태로 조회
     * 전우선 UtteranceService에서 IN_PROGRESS 세션 조회 시 사용
     */
    Optional<StorySession> findByIdAndStatus(Long id, String status);

    /**
     * 아이의 세션 목록 조회 (최근 활동 순)
     */
    List<StorySession> findByChildIdOrderByLastActivityAtDesc(Long childId);

    /**
     * 아이 + 이야기 + 상태로 진행 중인 세션 조회
     * 중복 세션 여부 확인 시 사용 가능
     */
    Optional<StorySession> findByChildIdAndStoryIdAndStatus(Long childId, Long storyId, String status);

    /**
     * 아이의 특정 상태 세션 중 가장 최근 활동한 1건 조회 (홈 이어하기용)
     */
    Optional<StorySession> findFirstByChildIdAndStatusOrderByLastActivityAtDesc(Long childId, String status);
}
