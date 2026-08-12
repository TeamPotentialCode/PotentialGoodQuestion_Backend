package com.potential.goodquestion.domain.storysession.dto;

import com.potential.goodquestion.domain.message.entity.Message;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 스토리 세션 응답 DTO 모음
 */
public class StorySessionResponseDto {

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
         * 대화 메시지 이력 (GET /api/sessions/{sessionId} 응답에만 포함)
         * POST 세션 생성 응답 시에는 null
         */
        private List<MessageInfo> messages;

        /**
         * StorySession 엔티티 → SessionInfo 변환 팩토리 (메시지 미포함)
         */
        public static SessionInfo from(StorySession session) {
            return SessionInfo.builder()
                    .sessionId(session.getId())
                    .storyId(session.getStory().getId())
                    .storyTitle(session.getStory().getTitle())
                    .childId(session.getChild().getId())
                    .childName(session.getChild().getName())
                    .status(session.getStatus())
                    .currentSceneId(session.getCurrentScene() != null ? session.getCurrentScene().getId() : null)
                    .currentChildTurnCount(session.getCurrentChildTurnCount())
                    .startedAt(session.getStartedAt())
                    .completedAt(session.getCompletedAt())
                    .build();
        }

        /**
         * StorySession 엔티티 → SessionInfo 변환 팩토리 (메시지 포함)
         * GET /api/sessions/{sessionId} 응답에 사용
         */
        public static SessionInfo from(StorySession session, List<MessageInfo> messages) {
            return SessionInfo.builder()
                    .sessionId(session.getId())
                    .storyId(session.getStory().getId())
                    .storyTitle(session.getStory().getTitle())
                    .childId(session.getChild().getId())
                    .childName(session.getChild().getName())
                    .status(session.getStatus())
                    .currentSceneId(session.getCurrentScene() != null ? session.getCurrentScene().getId() : null)
                    .currentChildTurnCount(session.getCurrentChildTurnCount())
                    .startedAt(session.getStartedAt())
                    .completedAt(session.getCompletedAt())
                    .messages(messages)
                    .build();
        }
    }

    /**
     * 대화 메시지 정보
     * GET /api/sessions/{sessionId} 응답의 messages 리스트 항목
     */
    @Getter
    @Builder
    public static class MessageInfo {

        /** 메시지 ID */
        private Long messageId;

        /** 발화자 (CHILD / CHARACTER) */
        private String speakerType;

        /** 대화 내용 */
        private String text;

        /** STT 원본 텍스트 (아이 발화만 해당, 나머지 null) */
        private String sttRawText;

        /** 장면 ID */
        private Long sceneId;

        /** 세션 내 발화 순서 */
        private int turnOrder;

        /**
         * Message 엔티티 → MessageInfo 변환 팩토리
         */
        public static MessageInfo from(Message message) {
            return MessageInfo.builder()
                    .messageId(message.getId())
                    .speakerType(message.getSpeakerType().name())
                    .text(message.getText())
                    .sttRawText(message.getSttRawText())
                    .sceneId(message.getScene().getId())
                    .turnOrder(message.getTurnOrder())
                    .build();
        }
    }
}
