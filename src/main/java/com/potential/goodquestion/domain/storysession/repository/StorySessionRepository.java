package com.potential.goodquestion.domain.storysession.repository;

import com.potential.goodquestion.domain.storysession.entity.StorySession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 상태별 세션 수 집계 (관리자 대시보드용)
     */
    long countByStatus(String status);

    /**
     * 특정 기간 내 시작된 세션 수 (오늘 세션 수 집계용)
     */
    long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 보호자의 모든 세션 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     * 참조하는 messages, post_activity_results 를 먼저 삭제한 뒤 호출해야 한다.
     */
    @Modifying
    @Query("DELETE FROM StorySession s WHERE s.child.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);

    /**
     * 아이의 모든 세션 삭제 (아이 프로필 삭제 시 연관 데이터 정리용)
     * 참조하는 messages, post_activity_results 를 먼저 삭제한 뒤 호출해야 한다.
     */
    @Modifying
    @Query("DELETE FROM StorySession s WHERE s.child.id = :childId")
    void deleteByChildId(@Param("childId") Long childId);
}
