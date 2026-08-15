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
     * 공개 상태 이야기 중 특정 주제를 가진 목록 조회 (등록 순)
     *
     * topics 는 JSON 배열 문자열( ["다름","자기이해"] )이므로 주제를 큰따옴표로 감싸
     * 배열 원소 단위로 매칭한다. 이렇게 하지 않으면 "자기" 처럼 원소의 일부만 보내도
     * "자기이해" 가 걸리는 부분 일치가 발생한다.
     *
     * topic 의 LIKE 와일드카드(% _ 및 이스케이프 문자)는 서비스에서 이스케이프해 전달한다.
     * ESCAPE 문자로는 백슬래시 대신 '!' 를 사용한다 (JPQL/SQL 이중 이스케이프 회피).
     */
    @Query("SELECT s FROM Story s "
            + "WHERE s.status = :status "
            + "AND s.topics LIKE CONCAT('%\"', :topic, '\"%') ESCAPE '!' "
            + "ORDER BY s.id ASC")
    List<Story> findByStatusAndTopicElement(@Param("status") String status,
                                            @Param("topic") String topic);

    /**
     * 상태별 이야기 수 집계 (관리자 대시보드용)
     */
    long countByStatus(String status);
}
