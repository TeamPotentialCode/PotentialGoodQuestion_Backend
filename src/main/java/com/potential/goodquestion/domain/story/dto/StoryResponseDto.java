package com.potential.goodquestion.domain.story.dto;

import com.potential.goodquestion.domain.story.entity.Story;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Story 응답 DTO 모음
 */
public class StoryResponseDto {

    /**
     * 이야기 목록 항목
     * GET /api/stories 에서 사용
     * MVP 목록 화면 표시 요소: 대표 이미지, 제목, 예상 시간, 주요 주제
     */
    @Getter
    @Builder
    public static class StorySummary {

        private Long storyId;
        private String title;
        private String thumbnailUrl;
        private Integer estimatedMinutes;
        private String difficulty;
        private List<String> topics;

        /**
         * Story 엔티티 → StorySummary 변환
         *
         * @param story  이야기 엔티티
         * @param topics JSON 문자열에서 파싱한 주제 목록
         */
        public static StorySummary of(Story story, List<String> topics) {
            return StorySummary.builder()
                    .storyId(story.getId())
                    .title(story.getTitle())
                    .thumbnailUrl(story.getThumbnailUrl())
                    .estimatedMinutes(story.getEstimatedMinutes())
                    .difficulty(story.getDifficulty())
                    .topics(topics)
                    .build();
        }
    }

    /**
     * 이야기 상세
     * GET /api/stories/{storyId} 에서 사용
     * MVP 상세 화면 표시 요소: 이야기 도입, 상황, 아이 역할
     */
    @Getter
    @Builder
    public static class StoryDetail {

        private Long storyId;
        private String title;
        private String summary;
        private String difficulty;
        private List<String> topics;
        private String thumbnailUrl;
        private Integer estimatedMinutes;
        private String introduction; // 도입 (배경/줄거리 소개)
        private String situation;    // 상황 (장면 배경 설명)
        private String childRole;    // 아이 역할 설명

        /**
         * Story 엔티티 → StoryDetail 변환
         *
         * @param story  이야기 엔티티
         * @param topics JSON 문자열에서 파싱한 주제 목록
         */
        public static StoryDetail of(Story story, List<String> topics) {
            return StoryDetail.builder()
                    .storyId(story.getId())
                    .title(story.getTitle())
                    .summary(story.getSummary())
                    .difficulty(story.getDifficulty())
                    .topics(topics)
                    .thumbnailUrl(story.getThumbnailUrl())
                    .estimatedMinutes(story.getEstimatedMinutes())
                    .introduction(story.getIntroduction())
                    .situation(story.getSituation())
                    .childRole(story.getChildRole())
                    .build();
        }
    }
}
