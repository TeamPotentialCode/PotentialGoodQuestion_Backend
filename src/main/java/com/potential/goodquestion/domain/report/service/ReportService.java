package com.potential.goodquestion.domain.report.service;

import com.potential.goodquestion.common.code.SessionErrorCode;
import com.potential.goodquestion.common.enums.SpeakerType;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.util.JsonUtils;
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
    private final JsonUtils jsonUtils;

    private static final Set<String> LOGIC_ELEMENTS = Set.of("REASON", "DECISION", "SOLUTION", "RESULT");
    private static final Set<String> EMPATHY_ELEMENTS = Set.of("EMOTION", "EMPATHY");
    private static final Set<String> PERSPECTIVE_ELEMENTS = Set.of("PERSPECTIVE", "REQUEST");

    /**
     * 보호자 리포트 조회
     *
     * 리포트에는 아이의 발화 원문이 포함되므로 세션의 아이가
     * 로그인한 보호자 소유인지 반드시 검증한다.
     *
     * @param sessionId 세션 ID
     * @param parentId  JWT에서 추출한 보호자 ID (소유권 검증용)
     */
    public ReportResponse getReport(Long sessionId, Long parentId) {
        StorySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        // 다른 보호자의 세션 리포트 접근 차단 (아동 발화 노출 방지)
        if (!session.getChild().getParent().getId().equals(parentId)) {
            throw new CustomException(SessionErrorCode.SESSION_ACCESS_DENIED);
        }

        List<StoryScene> scenes = sceneRepository.findByStoryIdOrderBySceneOrder(
                session.getStory().getId());

        // N+1 방지: 모든 분석 한 번에 로드 후 메모리에서 sceneId로 groupBy
        List<UtteranceAnalysis> allAnalyses = analysisRepository
                .findByMessageSessionIdOrderByCreatedAtAsc(sessionId);
        Map<Long, List<UtteranceAnalysis>> analysesByScene = allAnalyses.stream()
                .collect(Collectors.groupingBy(a -> a.getMessage().getScene().getId()));

        List<Message> childMessages = messageRepository
                .findBySessionIdAndSpeakerTypeOrderByCreatedAtAsc(sessionId, SpeakerType.CHILD);
        Map<Long, Long> turnCountByScene = childMessages.stream()
                .collect(Collectors.groupingBy(m -> m.getScene().getId(), Collectors.counting()));

        // 누적 요소 집계
        Set<String> accumulated = allAnalyses.stream()
                .flatMap(a -> jsonUtils.toDetectedTypes(a.getDetectedElements()).stream())
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> allRequired = scenes.stream()
                .flatMap(s -> jsonUtils.toStringList(s.getRequiredElements()).stream())
                .collect(Collectors.toSet());

        double achievementRate = allRequired.isEmpty() ? 0
                : (double) accumulated.size() / allRequired.size();

        // 장면별 리포트 (추가 DB 쿼리 없음)
        List<ReportResponse.SceneReport> sceneReports = scenes.stream().map(scene -> {
            List<UtteranceAnalysis> sceneAnalyses = analysesByScene.getOrDefault(scene.getId(), List.of());
            Set<String> sceneDetected = sceneAnalyses.stream()
                    .flatMap(a -> jsonUtils.toDetectedTypes(a.getDetectedElements()).stream())
                    .collect(Collectors.toSet());
            int turnCount = turnCountByScene.getOrDefault(scene.getId(), 0L).intValue();
            return new ReportResponse.SceneReport(
                    scene.getSceneOrder(), scene.getCharacterName(),
                    turnCount, session.getSceneEndReason(), new ArrayList<>(sceneDetected));
        }).toList();

        // 대표 발화 (요소 많은 순 상위 3개)
        List<ReportResponse.RepresentativeUtterance> repUtterances = allAnalyses.stream()
                .map(a -> {
                    List<String> elements = jsonUtils.toDetectedTypes(a.getDetectedElements());
                    int sceneOrder = scenes.stream()
                            .filter(s -> s.getId().equals(a.getMessage().getScene().getId()))
                            .findFirst().map(StoryScene::getSceneOrder).orElse(0);
                    return new ReportResponse.RepresentativeUtterance(
                            sceneOrder, a.getMessage().getText(), elements);
                })
                .filter(r -> !r.elements().isEmpty())
                .sorted((a, b) -> b.elements().size() - a.elements().size())
                .limit(3)
                .toList();

        // 학습 가이드
        Set<String> growthNeeded = new HashSet<>(allRequired);
        growthNeeded.removeAll(accumulated);
        int rate = allRequired.isEmpty() ? 0
                : (int) ((double) accumulated.size() / allRequired.size() * 100);

        return new ReportResponse(
                sessionId,
                session.getStory().getTitle(),
                session.getCompletedAt() != null ? session.getCompletedAt().toString() : null,
                new ReportResponse.ElementSummary(
                        new ArrayList<>(accumulated), allRequired.size(), achievementRate,
                        categoryScore(accumulated, LOGIC_ELEMENTS, allRequired),
                        categoryScore(accumulated, EMPATHY_ELEMENTS, allRequired),
                        categoryScore(accumulated, PERSPECTIVE_ELEMENTS, allRequired)
                ),
                sceneReports,
                repUtterances,
                new ReportResponse.LearningGuide(
                        "이번 이야기에서 " + accumulated.size() + "가지 사고 요소를 표현했어요. (목표 달성률 " + rate + "%)",
                        new ArrayList<>(accumulated),
                        new ArrayList<>(growthNeeded)
                )
        );
    }

    private ReportResponse.CategoryScore categoryScore(
            Set<String> accumulated, Set<String> category, Set<String> required) {
        List<String> detected = accumulated.stream().filter(category::contains).toList();
        int total = (int) required.stream().filter(category::contains).count();
        return new ReportResponse.CategoryScore(detected, Math.max(total, 1));
    }
}
