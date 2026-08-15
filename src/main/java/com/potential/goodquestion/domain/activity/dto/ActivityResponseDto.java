package com.potential.goodquestion.domain.activity.dto;

import com.potential.goodquestion.domain.activity.entity.Activity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 말하기 후 활동 응답 DTO 모음
 */
public class ActivityResponseDto {

    /**
     * 후 활동 시작 - 카드 제시
     * POST /api/sessions/{sessionId}/activity
     * 카드는 무작위 순서로 제시하며, 정답 순서(correct_order)는 포함하지 않는다.
     */
    @Getter
    @Builder
    public static class CardSet {

        private Long activityId;
        private Long sessionId;
        private List<Card> cards;

        public static CardSet of(Activity activity, List<Card> cards) {
            return CardSet.builder()
                    .activityId(activity.getId())
                    .sessionId(activity.getSession().getId())
                    .cards(cards)
                    .build();
        }

        @Getter
        @Builder
        public static class Card {
            private String id;
            private String text;
        }
    }

    /**
     * 후 활동 결과
     * GET   /api/sessions/{sessionId}/activity : 저장된 결과 조회
     * PATCH /api/sessions/{sessionId}/activity : 제출 직후 결과
     *
     * 정답일 때만 재구성용 핵심 단어(retellingKeywords)를 제공한다.
     * 아직 제출 전이면 attemptCount 가 0이고 submittedOrder 는 비어 있다.
     */
    @Getter
    @Builder
    public static class ActivityResult {

        private Long activityId;
        private Long sessionId;
        private List<String> submittedOrder;
        private int attemptCount;
        private boolean orderCorrect;
        private List<String> retellingKeywords;
        private String reconstructionText;
        private boolean completed;
        private LocalDateTime completedAt;

        /**
         * @param activity          활동 엔티티
         * @param retellingKeywords 정답일 때만 채워지는 핵심 단어 (오답이면 빈 목록)
         * @param submittedOrder    저장된 카드 순서 JSON 을 파싱한 결과
         */
        public static ActivityResult of(Activity activity, List<String> retellingKeywords,
                                        List<String> submittedOrder) {
            return ActivityResult.builder()
                    .activityId(activity.getId())
                    .sessionId(activity.getSession().getId())
                    .submittedOrder(submittedOrder)
                    .attemptCount(activity.getAttemptCount())
                    .orderCorrect(Boolean.TRUE.equals(activity.getIsOrderCorrect()))
                    .retellingKeywords(retellingKeywords)
                    .reconstructionText(activity.getRetellingText())
                    .completed(activity.getCompletedAt() != null)
                    .completedAt(activity.getCompletedAt())
                    .build();
        }
    }
}
