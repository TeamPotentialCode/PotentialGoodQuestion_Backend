package com.potential.goodquestion.common.engine.vo;

import java.util.HashSet;
import java.util.Set;

public record SessionState(
        int turnCount,
        int preferredTurns,
        int maxTurns,
        Set<String> accumulatedElements,
        Set<String> requiredElements,
        Set<String> newlyDetectedElements,
        String previousMode,
        int turnsWithoutNewElement,
        int consecutiveLowInformationTurns
) {
    public Set<String> missingElements() {
        Set<String> missing = new HashSet<>(requiredElements);
        missing.removeAll(accumulatedElements);
        return missing;
    }

    public boolean isFirstTurn() { return turnCount == 1; }
    public boolean hasNewlyDetected() { return !newlyDetectedElements.isEmpty(); }
    public boolean wasGuidedLastTurn() { return "GUIDED".equals(previousMode); }
    public int remainingTurns() { return maxTurns - turnCount; }
}
