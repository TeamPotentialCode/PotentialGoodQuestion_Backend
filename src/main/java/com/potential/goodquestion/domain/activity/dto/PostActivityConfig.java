package com.potential.goodquestion.domain.activity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * stories.post_activity_config(JSON) 역직렬화 대상
 *
 * 예시:
 * {
 *   "cards": [ { "id": "card_1", "text": "...", "correct_order": 1 }, ... ],
 *   "retelling_keywords": [ "며느리", "방귀", ... ]
 * }
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostActivityConfig {

    private List<Card> cards = new ArrayList<>();

    @JsonProperty("retelling_keywords")
    private List<String> retellingKeywords = new ArrayList<>();

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Card {

        private String id;
        private String text;

        /** 정답 순서 (1부터). 서버 정답 판정에만 사용하고 카드 제시 응답에는 노출하지 않는다. */
        @JsonProperty("correct_order")
        private Integer correctOrder;
    }
}
