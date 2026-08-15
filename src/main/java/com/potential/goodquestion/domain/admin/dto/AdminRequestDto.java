package com.potential.goodquestion.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * 관리자 API 요청 DTO 모음
 */
public class AdminRequestDto {

    /**
     * 관리자 로그인 요청
     */
    @Getter
    public static class Login {

        @NotBlank(message = "관리자 코드를 입력해주세요.")
        private String adminCode;
    }

    /**
     * 이야기 추가 요청
     */
    @Getter
    public static class CreateStory {

        @NotBlank(message = "이야기 제목을 입력해주세요.")
        private String title;

        private String summary;
        private String difficulty;

        /** 주제 목록 (예: ["다름", "자기이해"]) */
        private List<String> topics;

        private String thumbnailUrl;
        private String introduction;
        private String situation;
        private String childRole;
        private Integer estimatedMinutes;

        /** 말하기 후 활동 설정 JSON 문자열 (null 허용) */
        private String postActivityConfig;

        /** published / draft (미입력 시 published) */
        private String status;
    }

    /**
     * 이야기 수정 요청
     */
    @Getter
    public static class UpdateStory {

        @NotBlank(message = "이야기 제목을 입력해주세요.")
        private String title;

        private String summary;
        private String difficulty;
        private List<String> topics;
        private String thumbnailUrl;
        private String introduction;
        private String situation;
        private String childRole;
        private Integer estimatedMinutes;
        private String postActivityConfig;
        private String status;
    }

    /**
     * 이야기 장면 수정 요청
     */
    @Getter
    public static class UpdateScene {

        @NotNull(message = "장면 순서를 입력해주세요.")
        private Integer sceneOrder;

        @NotBlank(message = "장면 상황 설명을 입력해주세요.")
        private String sceneDescription;

        private String imageUrl;
        private String conflict;
        private String characterName;
        private String characterOpening;
        private String characterClosing;
        private String sceneGoal;
        private List<String> requiredElements;
        private Map<String, String> elementCriteria;
        private Map<String, String> remainingWorries;
        private boolean hasMission;

        @NotNull(message = "권장 최소 턴 수를 입력해주세요.")
        private Integer preferredTurns;

        @NotNull(message = "최대 허용 턴 수를 입력해주세요.")
        private Integer maxTurns;
    }

    /**
     * 이야기 장면 추가 요청
     */
    @Getter
    public static class CreateScene {

        @NotNull(message = "장면 순서를 입력해주세요.")
        private Integer sceneOrder;

        @NotBlank(message = "장면 상황 설명을 입력해주세요.")
        private String sceneDescription;

        private String imageUrl;
        private String conflict;

        /** null이면 내레이션 장면, non-null이면 대화 장면 */
        private String characterName;

        private String characterOpening;
        private String characterClosing;
        private String sceneGoal;

        /** 필수 사고 요소 목록 (예: ["REASON", "PERSPECTIVE"]) */
        private List<String> requiredElements;

        /** 요소별 인정 기준 (예: {"REASON": "이유를 구체적으로 설명했는지"}) */
        private Map<String, String> elementCriteria;

        /** 유도 시 사용할 캐릭터 걱정 정보 */
        private Map<String, String> remainingWorries;

        private boolean hasMission;

        @NotNull(message = "권장 최소 턴 수를 입력해주세요.")
        private Integer preferredTurns;

        @NotNull(message = "최대 허용 턴 수를 입력해주세요.")
        private Integer maxTurns;
    }
}
