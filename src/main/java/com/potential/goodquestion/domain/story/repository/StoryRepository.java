package com.potential.goodquestion.domain.story.repository;

import com.potential.goodquestion.domain.story.entity.Story;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Story 레포지토리
 */
public interface StoryRepository extends JpaRepository<Story, Long> {

    /**
     * 공개 상태 이야기 목록 조회 (등록 순)
     * 주제 필터는 topics JSON 파싱이 필요해 StoryService 에서 수행한다.
     */
    List<Story> findByStatusOrderByIdAsc(String status);

    /**
     * 상태별 이야기 수 집계 (관리자 대시보드용)
     */
    long countByStatus(String status);
}
