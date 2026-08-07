package com.potential.goodquestion.domain.utterance.service;

import com.potential.goodquestion.common.code.AiErrorCode;
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
import com.potential.goodquestion.common.util.JsonUtils;
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
    private final JsonUtils jsonUtils;

    public UtteranceResponse processUtterance(Long sessionId, UtteranceRequest request) {
        StorySession session = sessionRepository.findByIdAndStatus(sessionId, "IN_PROGRESS")
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        StoryScene scene = sceneRepository.findById(request.sceneId())
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        String childName = session.getChild().getName();
        String prevCharacterMsg = messageRepository
                .findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
                        sessionId, scene.getId(), SpeakerType.CHARACTER)
                .map(Message::getText)
                .orElse(null);

        Message childMessage = messageRepository.save(
                Message.ofChild(session, scene, request.text(), request.sttRawText()));

        AnalysisResponse rawAnalysis = callAnalysisLlm(scene, prevCharacterMsg, request.text());
        List<DetectedElement> processedElements = postProcessor.process(
                rawAnalysis.detectedElements(), request.text());

        analysisRepository.save(UtteranceAnalysis.builder()
                .message(childMessage)
                .childIntent(rawAnalysis.childIntent())
                .mainPoint(rawAnalysis.mainPoint())
                .detectedElements(jsonUtils.toJson(processedElements))
                .utteranceValidity(rawAnalysis.utteranceValidity())
                .build());

        Set<String> accumulated = new HashSet<>(jsonUtils.toStringSet(session.getAccumulatedElements()));
        Set<String> newlyDetected = processedElements.stream()
                .map(DetectedElement::type).collect(Collectors.toSet());
        accumulated.addAll(newlyDetected);

        boolean isLowInfo = isLowInformation(rawAnalysis.utteranceValidity());
        int newLowInfoCount = isLowInfo ? session.getConsecutiveLowInformationTurns() + 1 : 0;
        int newNoProgressCount = newlyDetected.isEmpty() ? session.getTurnsWithoutNewElement() + 1 : 0;

        SessionState state = new SessionState(
                session.getCurrentChildTurnCount() + 1,
                scene.getPreferredTurns(),
                scene.getMaxTurns(),
                accumulated,
                jsonUtils.toStringSet(scene.getRequiredElements()),
                newlyDetected,
                session.getLastResponseMode(),
                newNoProgressCount,
                newLowInfoCount
        );
        ProgressJudgeResult judgeResult = progressJudgeEngine.judge(state);

        String guidanceTarget = resolveGuidanceTarget(judgeResult, state, session, scene);

        session.updateAfterUtterance(
                jsonUtils.toJson(new ArrayList<>(accumulated)),
                jsonUtils.toJson(processedElements),
                judgeResult.mode().name(),
                guidanceTarget,
                newNoProgressCount,
                newLowInfoCount,
                judgeResult.isClosing() && judgeResult.closingReason() == ClosingReason.GOAL_MET,
                judgeResult.isClosing() ? judgeResult.closingReason().name() : null
        );

        Message characterMessage;
        boolean sceneCompleted = false;
        Long nextSceneId = null;

        if (judgeResult.isClosing()) {
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, replaceName(scene.getCharacterClosing(), childName)));
            sceneCompleted = true;
            nextSceneId = advanceOrComplete(session, scene);
        } else {
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, generateCharacterResponse(
                            scene, request.text(), rawAnalysis.childIntent(),
                            judgeResult, guidanceTarget, prevCharacterMsg)));
        }

        boolean showMission = resolveMissionDisplay(scene, state, newlyDetected);

        return buildResponse(sessionId, scene, childMessage, rawAnalysis,
                processedElements, judgeResult, accumulated, state,
                characterMessage, sceneCompleted, nextSceneId, showMission);
    }

    private AnalysisResponse callAnalysisLlm(StoryScene scene, String prevCharMsg, String childUtterance) {
        try {
            return analysisLlmClient.analyze(new AnalysisRequest(
                    scene.getSceneDescription() + (scene.getConflict() != null ? "\n" + scene.getConflict() : ""),
                    scene.getSceneGoal(),
                    prevCharMsg,
                    childUtterance,
                    new ArrayList<>(jsonUtils.toStringSet(scene.getRequiredElements())),
                    jsonUtils.toStringMap(scene.getElementCriteria())
            ));
        } catch (Exception e) {
            throw new CustomException(AiErrorCode.ANALYSIS_FAILED);
        }
    }

    private String generateCharacterResponse(StoryScene scene, String childUtterance,
            String childIntent, ProgressJudgeResult judgeResult,
            String guidanceTarget, String prevCharMsg) {
        try {
            Map<String, String> worries = jsonUtils.toStringMap(scene.getRemainingWorries());
            return characterResponseClient.generate(new CharacterRequest(
                    scene.getCharacterName(),
                    scene.getSceneDescription(),
                    childUtterance,
                    childIntent,
                    judgeResult.mode().name(),
                    guidanceTarget,
                    guidanceTarget != null ? worries.get(guidanceTarget) : null,
                    prevCharMsg
            )).text();
        } catch (Exception e) {
            throw new CustomException(AiErrorCode.CHARACTER_RESPONSE_FAILED);
        }
    }

    private String resolveGuidanceTarget(ProgressJudgeResult judgeResult,
            SessionState state, StorySession session, StoryScene scene) {
        if (judgeResult.mode() != ResponseMode.GUIDED) return null;
        return guidanceTargetSelector.select(
                state.missingElements(),
                session.getLastGuidanceTarget(),
                new ArrayList<>(jsonUtils.toStringSet(scene.getRequiredElements()))
        );
    }

    private Long advanceOrComplete(StorySession session, StoryScene currentScene) {
        // 다음 대화 장면 탐색 (character_name이 있는 장면)
        return sceneRepository.findByStoryIdOrderBySceneOrder(currentScene.getStory().getId())
                .stream()
                .filter(s -> s.getSceneOrder() > currentScene.getSceneOrder())
                .filter(s -> s.getCharacterName() != null)
                .findFirst()
                .map(next -> { session.advanceScene(next); return next.getId(); })
                .orElseGet(() -> { session.complete(); return null; });
    }

    private boolean resolveMissionDisplay(StoryScene scene, SessionState state, Set<String> newlyDetected) {
        if (!scene.isHasMission() || judgeResult_isClosing(state)) return false;
        // 미션2 (대화4): 첫 발화 이후 노출
        if (state.accumulatedElements().contains("EMOTION") || state.accumulatedElements().contains("PERSPECTIVE")) {
            return true;
        }
        // 미션1 (대화3): SOLUTION 탐지됐거나 2턴 이상 경과 후에도 SOLUTION 없을 때
        return newlyDetected.contains("SOLUTION")
                || (state.turnCount() >= 2 && !state.accumulatedElements().contains("SOLUTION"));
    }

    private boolean judgeResult_isClosing(SessionState state) {
        return state.missingElements().isEmpty() && state.turnCount() >= state.preferredTurns()
                || state.turnCount() >= state.maxTurns();
    }

    private String replaceName(String text, String childName) {
        if (text == null) return null;
        return text.replace("ㅇㅇ", childName);
    }

    private boolean isLowInformation(String validity) {
        return "SHORT".equals(validity) || "UNCLEAR".equals(validity) || "OFF_TOPIC".equals(validity);
    }

    private UtteranceResponse buildResponse(
            Long sessionId, StoryScene scene, Message childMessage,
            AnalysisResponse rawAnalysis, List<DetectedElement> processedElements,
            ProgressJudgeResult judgeResult, Set<String> accumulated,
            SessionState state, Message characterMessage,
            boolean sceneCompleted, Long nextSceneId, boolean showMission) {
        return new UtteranceResponse(
                sessionId, scene.getId(), childMessage.getId(),
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
                        new ArrayList<>(state.missingElements())
                ),
                new UtteranceResponse.CharacterMessageResult(
                        characterMessage.getId(),
                        characterMessage.getText(),
                        sceneCompleted
                ),
                sceneCompleted, nextSceneId, showMission
        );
    }
}
