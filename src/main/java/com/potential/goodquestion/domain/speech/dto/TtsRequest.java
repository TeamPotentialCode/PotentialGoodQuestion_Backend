package com.potential.goodquestion.domain.speech.dto;

import jakarta.validation.constraints.NotBlank;

public record TtsRequest(@NotBlank String text) {}
