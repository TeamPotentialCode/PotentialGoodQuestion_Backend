package com.potential.goodquestion.domain.growth.service;

import com.potential.goodquestion.common.code.ChildErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.util.JsonUtils;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.growth.dto.GrowthResponseDto;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import com.potential.goodquestion.domain.utterance.entity.UtteranceAnalysis;
import com.potential.goodquestion.domain.utterance.repository.UtteranceAnalysisRepository;
import com.potential.goodquestion.domain.word.entity.Word;
import com.potential.goodquestion.domain.word.repository.WordRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사고 요소 성장 현황 서비스
 *
 * 담당 API:
 * - GET /api/children/{childId}/growth : 아이의 세션별 사고 요소 누적 현황 조회
 *
 * 보안:
 * - parentId로 아이 소유권 검증 후 데이터 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrowthService {

    private final ChildRepository childRepository;
    private final StorySessionRepository sessionRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final WordRepository wordRepository;
    private final JsonUtils jsonUtils;

    // 서비스에서 추적하는 전체 사고 요소 목록 (8개)
    private static final List<String> ALL_ELEMENTS = List.of(
            "PERSPECTIVE", "REASON", "EMOTION", "SOLUTION",
            "RESULT", "EMPATHY", "REQUEST", "DECISION"
    );

    /**
     * 아이의 사고 요소 성장 현황 조회
     * 전체 세션에 걸쳐 각 사고 요소가 몇 번 탐지됐는지 집계한다.
     *
     * @param childId  아이 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     * @return 요소별 누적 탐지 횟수 + 세션별 요약 (최근 5개)
     */
    public GrowthResponseDto.GrowthInfo getGrowth(Long childId, Long parentId) {
        getChildWithOwnerCheck(parentId, childId);

        // 아이의 전체 세션 목록 (최근 활동 순)
        List<StorySession> sessions = sessionRepository.findByChildIdOrderByLastActivityAtDesc(childId);

        // 아이 전체 발화 분석을 한 번에 로드 (message, session fetch join으로 N+1 방지)
        List<UtteranceAnalysis> allAnalyses = analysisRepository.findByChildIdWithMessageAndSession(childId);

        // 세션 ID별로 탐지 요소 그룹핑
        Map<Long, Set<String>> elementsBySession = allAnalyses.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getMessage().getSession().getId(),
                        Collectors.flatMapping(
                                a -> jsonUtils.toDetectedTypes(a.getDetectedElements()).stream(),
                                Collectors.toSet()
                        )
                ));

        // 8개 요소별 누적 탐지 횟수 집계
        Map<String, Integer> elementCounts = buildElementCounts(elementsBySession);

        // 완료된 세션 수
        long completedCount = sessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .count();

        // 최근 5개 세션 요약
        List<GrowthResponseDto.SessionSummary> recentSessions = sessions.stream()
                .limit(5)
                .map(session -> GrowthResponseDto.SessionSummary.builder()
                        .sessionId(session.getId())
                        .storyTitle(session.getStory().getTitle())
                        .status(session.getStatus())
                        .completedAt(session.getCompletedAt() != null
                                ? session.getCompletedAt().toString() : null)
                        .detectedElements(List.copyOf(
                                elementsBySession.getOrDefault(session.getId(), Set.of())))
                        .build())
                .toList();

        // 단어장 현황 집계
        GrowthResponseDto.WordStats wordStats = buildWordStats(childId);

        return GrowthResponseDto.GrowthInfo.builder()
                .totalSessions(sessions.size())
                .completedSessions((int) completedCount)
                .elementCounts(elementCounts)
                .recentSessions(recentSessions)
                .wordStats(wordStats)
                .build();
    }

    // ─────────── private ────────────────

    /**
     * 단어장 현황 집계
     * 전체 저장 단어 수, 즐겨찾기 수, 최근 저장 단어 5개를 반환한다.
     * "이런 단어가 어려웠어요" 섹션에 사용
     */
    private GrowthResponseDto.WordStats buildWordStats(Long childId) {
        List<Word> words = wordRepository.findByChildIdOrderByCreatedAtDesc(childId);
        long favoriteCount = words.stream().filter(Word::isFavorite).count();
        long learnedCount = words.stream().filter(Word::isLearned).count();

        // 최근 저장 단어 최대 5개 (단어 + 쉬운 뜻만 포함)
        List<GrowthResponseDto.HardWord> recentWords = words.stream()
                .limit(5)
                .map(w -> GrowthResponseDto.HardWord.builder()
                        .word(w.getWord())
                        .meaning(w.getMeaning())
                        .savedAt(w.getCreatedAt())
                        .build())
                .toList();

        return GrowthResponseDto.WordStats.builder()
                .totalWords(words.size())
                .favoriteWords((int) favoriteCount)
                .learnedWords((int) learnedCount)
                .recentWords(recentWords)
                .build();
    }

    /**
     * 세션별 탐지 요소 맵을 집계해 요소 코드별 누적 탐지 횟수를 반환한다.
     * 탐지된 적 없는 요소도 0으로 포함 (레이더 차트 8각형 유지용)
     */
    private Map<String, Integer> buildElementCounts(Map<Long, Set<String>> elementsBySession) {
        Map<String, Integer> counts = new HashMap<>();

        // 모든 요소를 0으로 초기화
        ALL_ELEMENTS.forEach(element -> counts.put(element, 0));

        // 세션별 탐지 요소를 누적 집계
        elementsBySession.values().forEach(elements ->
                elements.forEach(element ->
                        counts.merge(element, 1, Integer::sum)
                )
        );
        return counts;
    }

    /**
     * 아이 조회 + 보호자 소유권 검증
     */
    private Child getChildWithOwnerCheck(Long parentId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CHILD_NOT_FOUND));

        // 로그인한 보호자의 아이가 아니면 403
        if (!child.getParent().getId().equals(parentId)) {
            throw new CustomException(ChildErrorCode.CHILD_ACCESS_DENIED);
        }
        return child;
    }
}
