package com.potential.goodquestion.domain.story.repository;

import com.potential.goodquestion.domain.story.entity.Story;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Story 레포지토리
 */
public interface StoryRepository extends JpaRepository<Story, Long> {

    /**
     * 주제로 이야기 목록 조회 (JSON 배열에 포함된 주제 필터링)
     */
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Story s WHERE s.topics LIKE %:topic%")
    List<Story> findAllByTopicContaining(@org.springframework.data.repository.query.Param("topic") String topic);
}
