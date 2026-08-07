package com.potential.goodquestion.domain.utterance.service;

import com.potential.goodquestion.common.engine.vo.DetectedElement;
import com.potential.goodquestion.common.openai.dto.AnalysisResponse;
import com.potential.goodquestion.common.util.JsonUtils;
import com.potential.goodquestion.domain.message.entity.Message;
import com.potential.goodquestion.domain.utterance.entity.UtteranceAnalysis;
import com.potential.goodquestion.domain.utterance.repository.UtteranceAnalysisRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UtteranceAnalysisAsyncSaver {

    private final UtteranceAnalysisRepository analysisRepository;
    private final JsonUtils jsonUtils;

    @Async("analysisExecutor")
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void save(Message childMessage, AnalysisResponse rawAnalysis,
                     List<DetectedElement> processedElements) {
        try {
            analysisRepository.save(UtteranceAnalysis.builder()
                    .message(childMessage)
                    .childIntent(rawAnalysis.childIntent())
                    .mainPoint(rawAnalysis.mainPoint())
                    .detectedElements(jsonUtils.toJson(processedElements))
                    .utteranceValidity(rawAnalysis.utteranceValidity())
                    .build());
        } catch (Exception e) {
            log.error("UtteranceAnalysis 비동기 저장 실패 messageId={}: {}", childMessage.getId(), e.getMessage());
        }
    }
}
