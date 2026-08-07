package com.potential.goodquestion.domain.utterance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.code.SessionErrorCode;
import com.potential.goodquestion.common.engine.GuidanceTargetSelector;
import com.potential.goodquestion.common.engine.PostProcessor;
import com.potential.goodquestion.common.engine.ProgressJudgeEngine;
import com.potential.goodquestion.common.engine.vo.DetectedElement;
import com.potential.goodquestion.common.engine.vo.ProgressJudgeResult;
import com.potential.goodquestion.common.engine.vo.SessionState;
import com.potential.goodquestion.common.enums.ClosingReason;
import com.potential.goodquestion.common.enums.ResponseMode;
import com.potential.goodquestion.common.enums.SpeakerType;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.openai.AnalysisLlmClient;
import com.potential.goodquestion.common.openai.CharacterResponseClient;
import com.potential.goodquestion.common.openai.dto.AnalysisRequest;
import com.potential.goodquestion.common.openai.dto.AnalysisResponse;
import com.potential.goodquestion.common.openai.dto.CharacterRequest;
import com.potential.goodquestion.common.openai.dto.CharacterResponse;
import com.potential.goodquestion.domain.message.entity.Message;
import com.potential.goodquestion.domain.message.repository.MessageRepository;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
import com.potential.goodquestion.domain.scene.repository.StorySceneRepository;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import com.potential.goodquestion.domain.utterance.dto.UtteranceRequest;
import com.potential.goodquestion.domain.utterance.dto.UtteranceResponse;
import com.potential.goodquestion.domain.utterance.entity.UtteranceAnalysis;
import com.potential.goodquestion.domain.utterance.repository.UtteranceAnalysisRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UtteranceService {

    private final StorySessionRepository sessionRepository;
    private final StorySceneRepository sceneRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final AnalysisLlmClient analysisLlmClient;
    private final CharacterResponseClient characterResponseClient;
    private final PostProcessor postProcessor;
    private final ProgressJudgeEngine progressJudgeEngine;
    private final GuidanceTargetSelector guidanceTargetSelector;
    private final ObjectMapper objectMapper;

    public UtteranceResponse processUtterance(Long sessionId, UtteranceRequest request) {
        // 1. 세션 + 장면 로드
        StorySession session = sessionRepository.findByIdAndStatus(sessionId, "IN_PROGRESS")
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        StoryScene scene = session.getCurrentScene();

        // 2. 직전 캐릭터 대사 조회
        String prevCharacterMsg = messageRepository
                .findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
                        sessionId, scene.getId(), SpeakerType.CHARACTER)
                .map(Message::getText)
                .orElse(null);

        // 3. 아이 메시지 저장
        Message childMessage = messageRepository.save(
                Message.ofChild(session, scene, request.text(), request.sttRawText()));

        // 4. LLM 분석
        AnalysisRequest analysisReq = buildAnalysisRequest(scene, prevCharacterMsg, request.text());
        AnalysisResponse rawAnalysis = analysisLlmClient.analyze(analysisReq);

        // 5. 서버 후처리
        List<DetectedElement> processedElements = postProcessor.process(
                rawAnalysis.detectedElements(), request.text());

        // 6. UtteranceAnalysis 저장
        analysisRepository.save(UtteranceAnalysis.builder()
                .message(childMessage)
                .childIntent(rawAnalysis.childIntent())
                .mainPoint(rawAnalysis.mainPoint())
                .detectedElements(toJson(processedElements))
                .utteranceValidity(rawAnalysis.utteranceValidity())
                .build());

        // 7. 누적 요소 갱신
        Set<String> accumulated = parseStringSet(session.getAccumulatedElements());
        Set<String> newlyDetected = processedElements.stream()
                .map(DetectedElement::type).collect(Collectors.toSet());
        accumulated.addAll(newlyDetected);
        Set<String> required = parseStringSet(scene.getRequiredElements());

        // 8. 저정보 카운터 갱신
        boolean isLowInfo = isLowInformation(rawAnalysis.utteranceValidity());
        int newLowInfoCount = isLowInfo ? session.getConsecutiveLowInformationTurns() + 1 : 0;
        int newNoProgressCount = newlyDetected.isEmpty() ? session.getTurnsWithoutNewElement() + 1 : 0;

        // 9. 진행 판단
        SessionState state = new SessionState(
                session.getCurrentChildTurnCount() + 1,
                scene.getPreferredTurns(),
                scene.getMaxTurns(),
                accumulated,
                required,
                newlyDetected,
                session.getLastResponseMode(),
                newNoProgressCount,
                newLowInfoCount
        );
        ProgressJudgeResult judgeResult = progressJudgeEngine.judge(state);

        // 10. GUIDED이면 유도 대상 선택
        String guidanceTarget = null;
        if (judgeResult.mode() == ResponseMode.GUIDED) {
            List<String> preferredOrder = new ArrayList<>(required);
            guidanceTarget = guidanceTargetSelector.select(
                    state.missingElements(),
                    session.getLastGuidanceTarget(),
                    preferredOrder
            );
        }

        // 11. 세션 상태 갱신
        session.updateAfterUtterance(
                toJson(new ArrayList<>(accumulated)),
                toJson(processedElements),
                judgeResult.mode().name(),
                guidanceTarget,
                newNoProgressCount,
                newLowInfoCount,
                judgeResult.isClosing() && judgeResult.closingReason() == ClosingReason.GOAL_MET,
                judgeResult.isClosing() ? judgeResult.closingReason().name() : null
        );

        // 12. 캐릭터 대사 결정
        Message characterMessage;
        boolean sceneCompleted = false;
        Long nextSceneId = null;

        if (judgeResult.isClosing()) {
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, scene.getCharacterClosing()));
            sceneCompleted = true;

            StoryScene nextScene = sceneRepository
                    .findByStoryIdAndSceneOrder(scene.getStory().getId(), scene.getSceneOrder() + 1)
                    .orElse(null);
            if (nextScene != null) {
                session.advanceScene(nextScene);
                nextSceneId = nextScene.getId();
            } else {
                session.complete();
            }
        } else {
            String worryForTarget = null;
            if (guidanceTarget != null) {
                Map<String, String> worries = parseStringMap(scene.getRemainingWorries());
                worryForTarget = worries.get(guidanceTarget);
            }
            CharacterResponse charResp = characterResponseClient.generate(new CharacterRequest(
                    scene.getCharacterName(),
                    scene.getSceneDescription(),
                    request.text(),
                    rawAnalysis.childIntent(),
                    judgeResult.mode().name(),
                    guidanceTarget,
                    worryForTarget,
                    prevCharacterMsg
            ));
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, charResp.text()));
        }

        // 13. 응답 조립
        List<String> missingList = new ArrayList<>(state.missingElements());
        return new UtteranceResponse(
                sessionId,
                scene.getId(),
                childMessage.getId(),
                new UtteranceResponse.AnalysisResult(
                        rawAnalysis.childIntent(),
                        processedElements.stream()
                                .map(e -> new UtteranceResponse.DetectedElementDto(e.type(), e.evidence()))
                                .toList(),
                        rawAnalysis.utteranceValidity()
                ),
                new UtteranceResponse.ProgressResult(
                        judgeResult.mode().name(),
                        new ArrayList<>(accumulated),
                        missingList
                ),
                new UtteranceResponse.CharacterMessageResult(
                        characterMessage.getId(),
                        characterMessage.getText(),
                        sceneCompleted
                ),
                sceneCompleted,
                nextSceneId
        );
    }

    private boolean isLowInformation(String validity) {
        return "SHORT".equals(validity) || "UNCLEAR".equals(validity) || "OFF_TOPIC".equals(validity);
    }

    private AnalysisRequest buildAnalysisRequest(StoryScene scene, String prevCharMsg, String childUtterance) {
        Map<String, String> criteria = parseStringMap(scene.getElementCriteria());
        List<String> targetElements = new ArrayList<>(parseStringSet(scene.getRequiredElements()));
        return new AnalysisRequest(
                scene.getSceneDescription() + (scene.getConflict() != null ? "\n" + scene.getConflict() : ""),
                scene.getSceneGoal(),
                prevCharMsg,
                childUtterance,
                targetElements,
                criteria
        );
    }

    private Set<String> parseStringSet(String json) {
        try {
            if (json == null || json.isBlank()) return new HashSet<>();
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {});
            return new HashSet<>(list);
        } catch (Exception e) { return new HashSet<>(); }
    }

    private Map<String, String> parseStringMap(String json) {
        try {
            if (json == null || json.isBlank()) return new HashMap<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) { return new HashMap<>(); }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }
}
