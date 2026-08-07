package com.potential.goodquestion.domain.utterance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UtteranceRequest(
        @NotNull Long sceneId,
        @NotBlank @Size(max = 500, message = "발화 텍스트는 500자 이하여야 합니다.") String text,
        String sttRawText
) {}
