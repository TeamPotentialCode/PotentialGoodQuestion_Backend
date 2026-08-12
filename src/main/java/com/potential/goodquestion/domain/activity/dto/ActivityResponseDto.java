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
     * 후 활동 완료 결과
     * PATCH /api/sessions/{sessionId}/activity
     * 정답일 때만 재구성용 핵심 단어(retellingKeywords)를 제공한다.
     */
    @Getter
    @Builder
    public static class ActivityResult {

        private Long activityId;
        private Long sessionId;
        private boolean orderCorrect;
        private List<String> retellingKeywords;
        private String reconstructionText;
        private boolean completed;
        private LocalDateTime completedAt;

        public static ActivityResult of(Activity activity, List<String> retellingKeywords) {
            return ActivityResult.builder()
                    .activityId(activity.getId())
                    .sessionId(activity.getSession().getId())
                    .orderCorrect(Boolean.TRUE.equals(activity.getIsOrderCorrect()))
                    .retellingKeywords(retellingKeywords)
                    .reconstructionText(activity.getRetellingText())
                    .completed(activity.getCompletedAt() != null)
                    .completedAt(activity.getCompletedAt())
                    .build();
        }
    }
}
