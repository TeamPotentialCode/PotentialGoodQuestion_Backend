package com.potential.goodquestion.domain.storysession.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스토리 세션 요청 DTO 모음
 */
public class StorySessionRequestDto {

    /**
     * 세션 생성 요청 (POST /api/stories/{storyId}/sessions)
     *
     * childId: JWT 보호자 계정에 속한 아이 ID
     *          보호자가 아이를 선택하여 이야기 학습을 시작할 때 전달
     */
    @Getter
    @NoArgsConstructor
    public static class Create {

        @NotNull(message = "아이 ID는 필수입니다.")
        private Long childId;
    }
}
