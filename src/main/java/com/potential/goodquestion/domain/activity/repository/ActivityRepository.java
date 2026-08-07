package com.potential.goodquestion.domain.activity.repository;

import com.potential.goodquestion.domain.activity.entity.Activity;
import com.potential.goodquestion.domain.session.entity.StorySession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Activity 레포지토리
 */
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    /**
     * 세션으로 활동 조회
     */
    Optional<Activity> findBySession(StorySession session);
}
