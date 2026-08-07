package com.potential.goodquestion.domain.report.dto;

import java.util.List;

public record ReportResponse(
        Long sessionId,
        String storyTitle,
        String completedAt,
        ElementSummary elementSummary,
        List<SceneReport> scenes,
        List<RepresentativeUtterance> representativeUtterances,
        LearningGuide learningGuide
) {
    public record ElementSummary(
            List<String> accumulated,
            int totalRequired,
            double achievementRate,
            CategoryScore logic,
            CategoryScore empathy,
            CategoryScore perspective
    ) {}

    public record CategoryScore(List<String> detected, int total) {
        public double rate() { return total == 0 ? 0 : (double) detected.size() / total; }
    }

    public record SceneReport(
            int sceneOrder,
            String characterName,
            int turnCount,
            String endReason,
            List<String> detectedElements
    ) {}

    public record RepresentativeUtterance(
            int sceneOrder,
            String text,
            List<String> elements
    ) {}

    public record LearningGuide(
            String summary,
            List<String> strengthElements,
            List<String> growthElements
    ) {}
}
