package com.potential.goodquestion.domain.utterance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "아이 발화 요청")
public record UtteranceRequest(
        @Schema(description = "현재 대화 중인 장면 ID (필수)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long sceneId,

        @Schema(description = "아이 최종 발화 텍스트 (필수, 최대 500자)", example = "며느리가 창피해서 계속 참았던 것 같아요", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 500, message = "발화 텍스트는 500자 이하여야 합니다.") String text,

        @Schema(description = "STT 원본 텍스트 (선택, 음성 입력 시 함께 전송)", example = "며느리가창피해서계속참았던것같아요", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String sttRawText
) {}
