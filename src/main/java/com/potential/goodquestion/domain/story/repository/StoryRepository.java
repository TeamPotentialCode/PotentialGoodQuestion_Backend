package com.potential.goodquestion.domain.story.repository;

import com.potential.goodquestion.domain.story.entity.Story;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Story 레포지토리
 */
public interface StoryRepository extends JpaRepository<Story, Long> {

    /**
     * 공개 상태 이야기 목록 조회 (등록 순)
     */
    List<Story> findByStatusOrderByIdAsc(String status);

    /**
     * 공개 상태 이야기 중 특정 주제를 포함하는 목록 조회 (등록 순)
     * topics 는 JSON 배열 문자열( ["다름","자기이해"] )이므로 LIKE 로 부분 매칭한다.
     */
    @Query("SELECT s FROM Story s "
            + "WHERE s.status = :status AND s.topics LIKE %:topic% "
            + "ORDER BY s.id ASC")
    List<Story> findByStatusAndTopicContaining(@Param("status") String status,
                                               @Param("topic") String topic);
}
