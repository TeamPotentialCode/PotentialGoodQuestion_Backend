package com.potential.goodquestion.domain.session.dto;

import com.potential.goodquestion.domain.session.entity.StorySession;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 세션 응답 DTO 모음
 */
public class SessionResponseDto {

    /**
     * 세션 정보 응답
     * POST /api/stories/{storyId}/sessions → 생성된 세션 반환
     * GET  /api/sessions/{sessionId}       → 이어하기 복귀용 세션 정보 반환
     */
    @Getter
    @Builder
    public static class SessionInfo {

        /** 세션 ID */
        private Long sessionId;

        /** 이야기 ID */
        private Long storyId;

        /** 이야기 제목 */
        private String storyTitle;

        /** 학습 아이 ID */
        private Long childId;

        /** 학습 아이 이름 */
        private String childName;

        /** 세션 상태 (IN_PROGRESS / COMPLETED) */
        private String status;

        /**
         * 현재 진행 중인 장면 ID
         * ⚠️ 전우선 담당: StoryScene 엔티티 생성 후 장면 상세 정보로 확장 가능
         */
        private Long currentSceneId;

        /** 현재 장면 누적 발화 횟수 */
        private int currentChildTurnCount;

        /** 세션 시작 일시 */
        private LocalDateTime startedAt;

        /** 세션 완료 일시 (진행 중이면 null) */
        private LocalDateTime completedAt;

        /**
         * StorySession 엔티티 → SessionInfo 변환 팩토리
         */
        public static SessionInfo from(StorySession session) {
            return SessionInfo.builder()
                    .sessionId(session.getId())
                    .storyId(session.getStory().getId())
                    .storyTitle(session.getStory().getTitle())
                    .childId(session.getChild().getId())
                    .childName(session.getChild().getName())
                    .status(session.getStatus())
                    .currentSceneId(session.getCurrentSceneId())
                    .currentChildTurnCount(session.getCurrentChildTurnCount())
                    .startedAt(session.getStartedAt())
                    .completedAt(session.getCompletedAt())
                    .build();
        }
    }
}
