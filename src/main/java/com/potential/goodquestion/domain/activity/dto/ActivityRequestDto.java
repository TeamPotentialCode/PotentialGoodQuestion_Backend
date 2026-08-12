package com.potential.goodquestion.domain.activity.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 말하기 후 활동 요청 DTO 모음
 */
public class ActivityRequestDto {

    /**
     * 카드 순서 제출 및 재구성 텍스트 저장 요청
     * (PATCH /api/sessions/{sessionId}/activity)
     *
     * submittedOrder: 아이가 배열한 카드 id 순서 (예: ["card_2","card_1","card_3"])
     * reconstructionText: 핵심 단어를 활용해 다시 말한 이야기 (STT 변환 결과, 선택)
     */
    @Getter
    @NoArgsConstructor
    public static class Submit {

        @NotEmpty(message = "카드 순서는 필수입니다.")
        private List<String> submittedOrder;

        private String reconstructionText;
    }
}
