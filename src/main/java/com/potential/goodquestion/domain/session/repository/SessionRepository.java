package com.potential.goodquestion.domain.session.repository;

import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.session.entity.Session;
import com.potential.goodquestion.domain.session.enums.SessionStatus;
import com.potential.goodquestion.domain.story.entity.Story;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Session 레포지토리
 */
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * 아이의 전체 세션 목록 조회
     */
    List<Session> findAllByChild(Child child);

    /**
     * 아이의 진행 중인 세션 조회 (이어하기 용)
     */
    Optional<Session> findByChildAndStoryAndStatus(Child child, Story story, SessionStatus status);
}
