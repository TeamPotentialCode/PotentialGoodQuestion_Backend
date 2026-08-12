package com.potential.goodquestion.domain.utterance.dto;

import java.util.List;

public record UtteranceResponse(
        Long sessionId,
        Long sceneId,
        Long childMessageId,
        AnalysisResult analysisResult,
        ProgressResult progressResult,
        CharacterMessageResult characterMessage,
        boolean sceneCompleted,
        Long nextSceneId,
        boolean showMission
) {
    public record AnalysisResult(
            String childIntent,
            List<DetectedElementDto> detectedElements,
            String utteranceValidity
    ) {}

    public record DetectedElementDto(String type, String evidence) {}

    public record ProgressResult(
            String mode,
            List<String> accumulatedElements,
            List<String> missingElements,
            int remainingTurns
    ) {}

    public record CharacterMessageResult(
            Long messageId,
            String text,
            boolean isClosing
    ) {}
}
