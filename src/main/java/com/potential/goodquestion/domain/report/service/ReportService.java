package com.potential.goodquestion.domain.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.code.SessionErrorCode;
import com.potential.goodquestion.common.enums.SpeakerType;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.message.entity.Message;
import com.potential.goodquestion.domain.message.repository.MessageRepository;
import com.potential.goodquestion.domain.report.dto.ReportResponse;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
import com.potential.goodquestion.domain.scene.repository.StorySceneRepository;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
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
@Transactional(readOnly = true)
public class ReportService {

    private final StorySessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final StorySceneRepository sceneRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> LOGIC_ELEMENTS = Set.of("REASON", "DECISION", "SOLUTION", "RESULT");
    private static final Set<String> EMPATHY_ELEMENTS = Set.of("EMOTION", "EMPATHY");
    private static final Set<String> PERSPECTIVE_ELEMENTS = Set.of("PERSPECTIVE", "REQUEST");

    public ReportResponse getReport(Long sessionId) {
        StorySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        List<StoryScene> scenes = sceneRepository.findByStoryIdOrderBySceneOrder(
                session.getStory().getId());
        List<UtteranceAnalysis> allAnalyses = analysisRepository
                .findByMessageSessionIdOrderByCreatedAtAsc(sessionId);
        List<Message> childMessages = messageRepository
                .findBySessionIdAndSpeakerTypeOrderByCreatedAtAsc(sessionId, SpeakerType.CHILD);

        // 누적 요소 집계
        Set<String> accumulated = new HashSet<>();
        for (UtteranceAnalysis a : allAnalyses) {
            parseDetectedTypes(a.getDetectedElements()).forEach(accumulated::add);
        }

        Set<String> allRequired = scenes.stream()
                .flatMap(s -> parseStringList(s.getRequiredElements()).stream())
                .collect(Collectors.toSet());

        double achievementRate = allRequired.isEmpty() ? 0
                : (double) accumulated.size() / allRequired.size();

        // 카테고리별 점수
        ReportResponse.CategoryScore logicScore = categoryScore(accumulated, LOGIC_ELEMENTS, allRequired);
        ReportResponse.CategoryScore empathyScore = categoryScore(accumulated, EMPATHY_ELEMENTS, allRequired);
        ReportResponse.CategoryScore perspectiveScore = categoryScore(accumulated, PERSPECTIVE_ELEMENTS, allRequired);

        // 장면별 리포트
        List<ReportResponse.SceneReport> sceneReports = scenes.stream().map(scene -> {
            List<UtteranceAnalysis> sceneAnalyses = analysisRepository
                    .findByMessageSessionIdAndMessageSceneId(sessionId, scene.getId());
            Set<String> sceneDetected = new HashSet<>();
            sceneAnalyses.forEach(a -> parseDetectedTypes(a.getDetectedElements())
                    .forEach(sceneDetected::add));
            int turnCount = (int) childMessages.stream()
                    .filter(m -> m.getScene().getId().equals(scene.getId())).count();
            return new ReportResponse.SceneReport(
                    scene.getSceneOrder(),
                    scene.getCharacterName(),
                    turnCount,
                    session.getSceneEndReason(),
                    new ArrayList<>(sceneDetected)
            );
        }).toList();

        // 대표 발화 (요소 많은 순 상위 3개)
        List<ReportResponse.RepresentativeUtterance> repUtterances = allAnalyses.stream()
                .filter(a -> !parseDetectedTypes(a.getDetectedElements()).isEmpty())
                .map(a -> {
                    List<String> elements = parseDetectedTypes(a.getDetectedElements());
                    int sceneOrder = scenes.stream()
                            .filter(s -> s.getId().equals(a.getMessage().getScene().getId()))
                            .findFirst().map(StoryScene::getSceneOrder).orElse(0);
                    return new ReportResponse.RepresentativeUtterance(
                            sceneOrder, a.getMessage().getText(), elements);
                })
                .sorted((a, b) -> b.elements().size() - a.elements().size())
                .limit(3)
                .toList();

        // 학습 가이드
        Set<String> growthNeeded = new HashSet<>(allRequired);
        growthNeeded.removeAll(accumulated);
        int rate = allRequired.isEmpty() ? 0
                : (int) ((double) accumulated.size() / allRequired.size() * 100);
        ReportResponse.LearningGuide guide = new ReportResponse.LearningGuide(
                "이번 이야기에서 " + accumulated.size() + "가지 사고 요소를 표현했어요. (목표 달성률 " + rate + "%)",
                new ArrayList<>(accumulated),
                new ArrayList<>(growthNeeded)
        );

        return new ReportResponse(
                sessionId,
                session.getStory().getTitle(),
                session.getCompletedAt() != null ? session.getCompletedAt().toString() : null,
                new ReportResponse.ElementSummary(
                        new ArrayList<>(accumulated),
                        allRequired.size(),
                        achievementRate,
                        logicScore, empathyScore, perspectiveScore
                ),
                sceneReports,
                repUtterances,
                guide
        );
    }

    private ReportResponse.CategoryScore categoryScore(
            Set<String> accumulated, Set<String> category, Set<String> required) {
        List<String> detected = accumulated.stream().filter(category::contains).toList();
        int total = (int) required.stream().filter(category::contains).count();
        return new ReportResponse.CategoryScore(detected, Math.max(total, 1));
    }

    private List<String> parseDetectedTypes(String json) {
        try {
            if (json == null || json.isBlank() || "[]".equals(json)) return List.of();
            List<Map<String, String>> elements = objectMapper.readValue(json, new TypeReference<>() {});
            return elements.stream()
                    .map(e -> e.get("type"))
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
        } catch (Exception e) { return List.of(); }
    }

    private List<String> parseStringList(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) { return List.of(); }
    }
}
