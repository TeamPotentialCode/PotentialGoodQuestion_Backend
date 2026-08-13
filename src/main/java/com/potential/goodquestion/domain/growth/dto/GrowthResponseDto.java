package com.potential.goodquestion.domain.growth.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 사고 요소 성장 현황 응답 DTO 모음
 */
public class GrowthResponseDto {

    /**
     * 아이 성장 종합 현황 응답
     * GET /api/children/{childId}/growth
     * 사고 요소 성장 + 단어장 현황을 한 번에 제공한다.
     */
    @Getter
    @Builder
    public static class GrowthInfo {

        private int totalSessions;        // 전체 세션 수
        private int completedSessions;    // 완료된 세션 수

        /**
         * 8개 사고 요소별 누적 탐지 횟수 (레이더 차트용)
         * key: 요소 코드 (PERSPECTIVE, REASON, EMOTION 등)
         * value: 세션 단위 누적 탐지 횟수
         */
        private Map<String, Integer> elementCounts;

        /**
         * 세션별 발화 요약 (최근 5개)
         * 레이더 차트의 세션 간 변화 추이에 사용
         */
        private List<SessionSummary> recentSessions;

        /**
         * 단어장 현황 — "이런 단어가 어려웠어요" 섹션용
         */
        private WordStats wordStats;
    }

    /**
     * 세션별 사고 요소 요약
     */
    @Getter
    @Builder
    public static class SessionSummary {

        private Long sessionId;
        private String storyTitle;              // 이야기 제목
        private String status;                  // IN_PROGRESS / COMPLETED / ABANDONED
        private String completedAt;             // 완료 시각 (null 가능)
        private List<String> detectedElements;  // 해당 세션에서 탐지된 요소 목록
    }

    /**
     * 단어장 현황 요약
     * 보호자에게 "우리 아이가 어려워한 단어들이에요" 형태로 노출
     */
    @Getter
    @Builder
    public static class WordStats {

        private int totalWords;      // 전체 저장 단어 수
        private int favoriteWords;   // 즐겨찾기 단어 수
        private int learnedWords;    // 학습 완료 단어 수

        /**
         * 최근 저장된 단어 목록 (최대 5개)
         * 어떤 단어를 어려워했는지 한눈에 보이도록
         */
        private List<HardWord> recentWords;
    }

    /**
     * 단어 요약 (단어 + 쉬운 뜻만 포함)
     */
    @Getter
    @Builder
    public static class HardWord {

        private String word;         // 저장된 단어
        private String meaning;      // GPT 생성 쉬운 뜻
        private LocalDateTime savedAt;  // 저장 시각
    }
}
