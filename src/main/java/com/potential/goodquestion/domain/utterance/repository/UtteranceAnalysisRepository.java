package com.potential.goodquestion.domain.utterance.repository;

import com.potential.goodquestion.domain.utterance.entity.UtteranceAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
     */
    List<UtteranceAnalysis> findByMessageSessionChildId(Long childId);
}
