package com.potential.goodquestion.domain.speech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "TTS 요청")
public record TtsRequest(
        @Schema(description = "음성으로 변환할 텍스트 (필수)", example = "그래, 며느리도 많이 힘들었겠구나...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String text
) {}
