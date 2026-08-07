package com.potential.goodquestion.common.openai.dto;

public record CharacterRequest(
        String characterName,
        String sceneContext,
        String childUtterance,
        String childIntent,
        String responseMode,
        String guidanceTarget,
        String guidanceWorry,
        String previousCharacterMessage
) {}
