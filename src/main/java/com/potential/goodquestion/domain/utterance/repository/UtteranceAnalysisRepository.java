package com.potential.goodquestion.domain.utterance.repository;

import com.potential.goodquestion.domain.utterance.entity.UtteranceAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtteranceAnalysisRepository extends JpaRepository<UtteranceAnalysis, Long> {

    List<UtteranceAnalysis> findByMessageSessionIdOrderByCreatedAtAsc(Long sessionId);

    List<UtteranceAnalysis> findByMessageSessionIdAndMessageSceneId(Long sessionId, Long sceneId);
}
