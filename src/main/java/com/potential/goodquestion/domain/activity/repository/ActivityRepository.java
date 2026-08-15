package com.potential.goodquestion.domain.activity.repository;

import com.potential.goodquestion.domain.activity.entity.Activity;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Activity 레포지토리
 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /**
     * 세션으로 활동 조회
     */
    Optional<Activity> findBySession(StorySession session);

    /**
     * 세션 ID로 활동 조회
     */
    Optional<Activity> findBySessionId(Long sessionId);

    /**
     * 보호자의 모든 활동 결과 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     */
    @Modifying
    @Query("DELETE FROM Activity a WHERE a.session.child.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);
}
