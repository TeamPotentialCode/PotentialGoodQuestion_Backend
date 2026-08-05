package com.potential.goodquestion.domain.story.repository;

import com.potential.goodquestion.domain.story.entity.Story;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Story 레포지토리
 */
public interface StoryRepository extends JpaRepository<Story, Long> {

    /**
     * 주제로 이야기 목록 조회 (주제별 필터링)
     */
    List<Story> findAllByTopic(String topic);
}
