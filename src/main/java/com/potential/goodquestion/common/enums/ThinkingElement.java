package com.potential.goodquestion.common.enums;

public enum ThinkingElement {
    DECISION, REASON, PERSPECTIVE, SOLUTION, RESULT, EMOTION, EMPATHY, REQUEST;

    public static boolean isValid(String code) {
        try { valueOf(code); return true; } catch (IllegalArgumentException e) { return false; }
    }
}
