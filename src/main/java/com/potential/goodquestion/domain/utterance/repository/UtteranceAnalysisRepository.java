package com.potential.goodquestion.domain.utterance.repository;

import com.potential.goodquestion.domain.utterance.entity.UtteranceAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

public interface UtteranceAnalysisRepository extends JpaRepository<UtteranceAnalysis, Long> {

    /**
     * 세션 내 전체 발화 분석 조회 (오래된 순)
     */
    List<UtteranceAnalysis> findByMessageSessionIdOrderByCreatedAtAsc(Long sessionId);

    /**
     * 세션 + 장면 기준 발화 분석 조회
     */
    List<UtteranceAnalysis> findByMessageSessionIdAndMessageSceneId(Long sessionId, Long sceneId);

    /**
     * 아이의 전체 세션에 걸친 발화 분석 조회 (성장 레이더 차트 집계용)
     * message, session을 fetch join해 N+1 방지
     */
    @Query("SELECT ua FROM UtteranceAnalysis ua " +
           "JOIN FETCH ua.message m " +
           "JOIN FETCH m.session s " +
           "WHERE s.child.id = :childId")
    List<UtteranceAnalysis> findByChildIdWithMessageAndSession(@Param("childId") Long childId);

    /**
     * 보호자의 모든 발화 분석 삭제 (회원 탈퇴 시 연관 데이터 정리용)
     * message -> session -> child -> parent 로 이어지는 FK 체인의 최하위이므로 가장 먼저 삭제한다.
     */
    @Modifying
    @Query("DELETE FROM UtteranceAnalysis ua WHERE ua.message.id IN "
            + "(SELECT m.id FROM Message m WHERE m.session.child.parent.id = :parentId)")
    void deleteByParentId(@Param("parentId") Long parentId);

    /**
     * 아이의 모든 발화 분석 삭제 (아이 프로필 삭제 시 연관 데이터 정리용)
     * message -> session -> child 로 이어지는 FK 체인의 최하위이므로 가장 먼저 삭제한다.
     */
    @Modifying
    @Query("DELETE FROM UtteranceAnalysis ua WHERE ua.message.id IN "
            + "(SELECT m.id FROM Message m WHERE m.session.child.id = :childId)")
    void deleteByChildId(@Param("childId") Long childId);
}
