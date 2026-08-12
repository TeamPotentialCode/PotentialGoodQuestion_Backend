package com.potential.goodquestion.domain.home.dto;

import com.potential.goodquestion.domain.story.dto.StoryResponseDto;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 홈 화면 응답 DTO 모음
 */
public class HomeResponseDto {

    /**
     * 홈 화면
     * GET /api/home?childId=
     * - continueSession: 이어하기 대상 세션 (없으면 null)
     * - recommendedStories: 추천 이야기 (MVP: 공개 이야기 상위 N개, 추천 로직 미구현)
     */
    @Getter
    @Builder
    public static class HomeInfo {

        private ContinueInfo continueSession;
        private List<StoryResponseDto.StorySummary> recommendedStories;
    }

    /**
     * 이어하기 정보
     */
    @Getter
    @Builder
    public static class ContinueInfo {

        private Long sessionId;
        private Long storyId;
        private String storyTitle;
        private String thumbnailUrl;
        private Long currentSceneId;
        private Integer currentSceneOrder;
        private String status;
        private LocalDateTime lastActivityAt;

        public static ContinueInfo from(StorySession session) {
            return ContinueInfo.builder()
                    .sessionId(session.getId())
                    .storyId(session.getStory().getId())
                    .storyTitle(session.getStory().getTitle())
                    .thumbnailUrl(session.getStory().getThumbnailUrl())
                    .currentSceneId(session.getCurrentScene() != null ? session.getCurrentScene().getId() : null)
                    .currentSceneOrder(session.getCurrentScene() != null ? session.getCurrentScene().getSceneOrder() : null)
                    .status(session.getStatus())
                    .lastActivityAt(session.getLastActivityAt())
                    .build();
        }
    }
}
