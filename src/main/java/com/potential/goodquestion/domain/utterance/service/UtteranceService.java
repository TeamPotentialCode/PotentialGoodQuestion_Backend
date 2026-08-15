package com.potential.goodquestion.domain.utterance.service;

import com.potential.goodquestion.common.code.AiErrorCode;
import com.potential.goodquestion.common.code.SessionErrorCode;
import com.potential.goodquestion.common.engine.GuidanceTargetSelector;
import com.potential.goodquestion.common.engine.PostProcessor;
import com.potential.goodquestion.common.engine.ProgressJudgeEngine;
import com.potential.goodquestion.common.engine.ReactionKeyResolver;
import com.potential.goodquestion.common.engine.vo.DetectedElement;
import com.potential.goodquestion.common.engine.vo.ProgressJudgeResult;
import com.potential.goodquestion.common.engine.vo.SessionState;
import com.potential.goodquestion.common.enums.ClosingReason;
import com.potential.goodquestion.common.enums.ReactionKey;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional
public class UtteranceService {

    private final StorySessionRepository sessionRepository;
    private final StorySceneRepository sceneRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final UtteranceAnalysisAsyncSaver asyncSaver;
    private final AnalysisLlmClient analysisLlmClient;
    private final CharacterResponseClient characterResponseClient;
    private final PostProcessor postProcessor;
    private final ProgressJudgeEngine progressJudgeEngine;
    private final GuidanceTargetSelector guidanceTargetSelector;
    private final ReactionKeyResolver reactionKeyResolver;
    private final JsonUtils jsonUtils;

    public UtteranceResponse processUtterance(Long sessionId, UtteranceRequest request) {
        StorySession session = sessionRepository.findByIdAndStatus(sessionId, "IN_PROGRESS")
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        StoryScene scene = sceneRepository.findById(request.sceneId())
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        // 장면 JSON 필드 — 요청당 1회만 파싱
        Set<String> requiredElements = jsonUtils.toStringSet(scene.getRequiredElements());
        Map<String, String> elementCriteria = jsonUtils.toStringMap(scene.getElementCriteria());
        Map<String, String> remainingWorries = jsonUtils.toStringMap(scene.getRemainingWorries());

        String childName = session.getChild().getName();
        String prevCharacterMsg = messageRepository
                .findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
                        sessionId, scene.getId(), SpeakerType.CHARACTER)
                .map(Message::getText)
                .orElse(null);

        String sanitizedText = sanitize(request.text());
        int nextTurnOrder = (int) messageRepository.countBySessionId(sessionId) + 1;
        Message childMessage = messageRepository.save(
                Message.ofChild(session, scene, sanitizedText, request.sttRawText(), nextTurnOrder));

        AnalysisResponse rawAnalysis = callAnalysisLlm(
                scene, prevCharacterMsg, sanitizedText, requiredElements, elementCriteria);
        List<DetectedElement> processedElements = postProcessor.process(
                rawAnalysis.detectedElements(), sanitizedText);

        // 부모 트랜잭션 커밋 후 실행 — message가 DB에 존재한 뒤 FK INSERT 시도
        Message finalChildMessage = childMessage;
        List<DetectedElement> finalProcessedElements = processedElements;
        AnalysisResponse finalRawAnalysis = rawAnalysis;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncSaver.save(finalChildMessage, finalRawAnalysis, finalProcessedElements);
            }
        });

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
                requiredElements,
                newlyDetected,
                session.getLastResponseMode(),
                newNoProgressCount,
                newLowInfoCount
        );
        ProgressJudgeResult judgeResult = progressJudgeEngine.judge(state);

        String guidanceTarget = resolveGuidanceTarget(judgeResult, state, session, requiredElements);

        ReactionKey reactionKey = reactionKeyResolver.resolve(
                rawAnalysis.childIntent(), rawAnalysis.utteranceValidity(), processedElements);
        String softRemainingWorry = resolveSoftRemainingWorry(
                judgeResult, state, reactionKey, remainingWorries, requiredElements);

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
                    Message.ofCharacter(session, scene, replaceName(scene.getCharacterClosing(), childName), nextTurnOrder + 1));
            sceneCompleted = true;
            nextSceneId = advanceOrComplete(session, scene);
        } else {
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, generateCharacterResponse(
                            scene, request.text(), rawAnalysis.childIntent(),
                            judgeResult, guidanceTarget, prevCharacterMsg,
                            reactionKey, softRemainingWorry, remainingWorries), nextTurnOrder + 1));
        }

        boolean showMission = resolveMissionDisplay(scene, state, newlyDetected);
        String missionType = showMission ? resolveMissionType(scene) : null;

        return buildResponse(sessionId, scene, childMessage, rawAnalysis,
                processedElements, judgeResult, accumulated, state,
                characterMessage, sceneCompleted, nextSceneId, showMission, missionType);
    }

    private static final int SCENE_CONTEXT_MAX_LENGTH = 300;
    private static final int UTTERANCE_MAX_LENGTH = 500;

    private AnalysisResponse callAnalysisLlm(StoryScene scene, String prevCharMsg,
            String childUtterance, Set<String> requiredElements, Map<String, String> elementCriteria) {
        try {
            String sceneContext = scene.getSceneDescription() + (scene.getConflict() != null ? "\n" + scene.getConflict() : "");
            if (sceneContext.length() > SCENE_CONTEXT_MAX_LENGTH) {
                sceneContext = sceneContext.substring(0, SCENE_CONTEXT_MAX_LENGTH);
            }
            return analysisLlmClient.analyze(new AnalysisRequest(
                    sceneContext,
                    scene.getSceneGoal(),
                    prevCharMsg,
                    childUtterance,
                    new ArrayList<>(requiredElements),
                    elementCriteria
            ));
        } catch (Exception e) {
            throw new CustomException(AiErrorCode.ANALYSIS_FAILED);
        }
    }

    private String generateCharacterResponse(StoryScene scene, String childUtterance,
            String childIntent, ProgressJudgeResult judgeResult,
            String guidanceTarget, String prevCharMsg,
            ReactionKey reactionKey, String softRemainingWorry, Map<String, String> remainingWorries) {
        try {
            return characterResponseClient.generate(new CharacterRequest(
                    scene.getCharacterName(),
                    scene.getSceneDescription(),
                    childUtterance,
                    childIntent,
                    reactionKey.name(),
                    judgeResult.mode().name(),
                    guidanceTarget,
                    guidanceTarget != null ? remainingWorries.get(guidanceTarget) : null,
                    softRemainingWorry,
                    prevCharMsg
            )).text();
        } catch (Exception e) {
            throw new CustomException(AiErrorCode.CHARACTER_RESPONSE_FAILED);
        }
    }

    private String resolveGuidanceTarget(ProgressJudgeResult judgeResult,
            SessionState state, StorySession session, Set<String> requiredElements) {
        if (judgeResult.mode() != ResponseMode.GUIDED) return null;
        return guidanceTargetSelector.select(
                state.missingElements(),
                session.getLastGuidanceTarget(),
                new ArrayList<>(requiredElements)
        );
    }

    private String resolveSoftRemainingWorry(ProgressJudgeResult judgeResult, SessionState state,
            ReactionKey reactionKey, Map<String, String> remainingWorries, Set<String> requiredElements) {
        if (judgeResult.mode() != ResponseMode.NORMAL) return null;
        if (state.newlyDetectedElements().isEmpty()) return null;
        if (state.missingElements().isEmpty()) return null;
        if (reactionKey.isSoftCueSkip()) return null;

        String softTarget = guidanceTargetSelector.select(
                state.missingElements(), null,
                new ArrayList<>(requiredElements));
        if (softTarget == null) return null;
        return remainingWorries.get(softTarget);
    }

    private Long advanceOrComplete(StorySession session, StoryScene currentScene) {
        return sceneRepository.findByStoryIdOrderBySceneOrder(currentScene.getStory().getId())
                .stream()
                .filter(s -> s.getSceneOrder() > currentScene.getSceneOrder())
                .filter(s -> s.getCharacterName() != null)
                .findFirst()
                .map(next -> { session.advanceScene(next); return next.getId(); })
                .orElseGet(() -> { session.complete(); return null; });
    }

    private String resolveMissionType(StoryScene scene) {
        Set<String> required = jsonUtils.toStringSet(scene.getRequiredElements());
        return (required.contains("EMOTION") || required.contains("PERSPECTIVE"))
                ? "MISSION_2" : "MISSION_1";
    }

    private boolean resolveMissionDisplay(StoryScene scene, SessionState state, Set<String> newlyDetected) {
        if (!scene.isHasMission() || judgeResult_isClosing(state)) return false;
        if (state.requiredElements().contains("EMOTION") || state.requiredElements().contains("PERSPECTIVE")) {
            // 미션2 (대화4): EMOTION 또는 PERSPECTIVE가 누적 요소에 있을 때
            return state.accumulatedElements().contains("EMOTION")
                    || state.accumulatedElements().contains("PERSPECTIVE");
        }
        // 미션1 (대화3): SOLUTION 탐지됐거나 2턴 이상 경과 후에도 SOLUTION 없을 때
        return newlyDetected.contains("SOLUTION")
                || (state.turnCount() >= 2 && !state.accumulatedElements().contains("SOLUTION"));
    }

    private boolean judgeResult_isClosing(SessionState state) {
        return state.missingElements().isEmpty() && state.turnCount() >= state.preferredTurns()
                || state.turnCount() >= state.maxTurns();
    }

    private String sanitize(String text) {
        if (text == null) return "";
        String cleaned = text.strip().replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");
        if (cleaned.length() > UTTERANCE_MAX_LENGTH) {
            cleaned = cleaned.substring(0, UTTERANCE_MAX_LENGTH);
        }
        return cleaned;
    }

    private String replaceName(String text, String childName) {
        if (text == null) return null;
        return text.replace("ㅇㅇ", childName);
    }

    private boolean isLowInformation(String validity) {
        return "SHORT".equals(validity) || "UNCLEAR".equals(validity)
                || "OFF_TOPIC".equals(validity) || "PLAYFUL".equals(validity);
    }

    private UtteranceResponse buildResponse(
            Long sessionId, StoryScene scene, Message childMessage,
            AnalysisResponse rawAnalysis, List<DetectedElement> processedElements,
            ProgressJudgeResult judgeResult, Set<String> accumulated,
            SessionState state, Message characterMessage,
            boolean sceneCompleted, Long nextSceneId, boolean showMission, String missionType) {
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
                        new ArrayList<>(state.missingElements()),
                        state.remainingTurns()
                ),
                new UtteranceResponse.CharacterMessageResult(
                        characterMessage.getId(),
                        characterMessage.getText(),
                        sceneCompleted
                ),
                sceneCompleted, nextSceneId, showMission, missionType
        );
    }
}
