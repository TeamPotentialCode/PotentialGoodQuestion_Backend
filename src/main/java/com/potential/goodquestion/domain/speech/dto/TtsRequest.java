package com.potential.goodquestion.domain.speech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "TTS 요청")
public record TtsRequest(
        @Schema(description = "음성으로 변환할 텍스트 (필수)", example = "그래, 며느리도 많이 힘들었겠구나...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String text,

        @Schema(description = "목소리 선택 (선택, 기본값: nova)", example = "nova",
                allowableValues = {"alloy", "echo", "fable", "onyx", "nova", "shimmer"})
        @Pattern(regexp = "^(alloy|echo|fable|onyx|nova|shimmer)$", message = "지원하지 않는 목소리입니다.")
        String voice
) {}
