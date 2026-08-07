package com.potential.goodquestion.domain.utterance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UtteranceRequest(
        @NotNull Long sceneId,
        @NotBlank String text,
        String sttRawText
) {}
