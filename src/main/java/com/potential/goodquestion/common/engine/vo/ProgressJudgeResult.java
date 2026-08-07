package com.potential.goodquestion.common.engine.vo;

import com.potential.goodquestion.common.enums.ClosingReason;
import com.potential.goodquestion.common.enums.ResponseMode;

public record ProgressJudgeResult(
        ResponseMode mode,
        ClosingReason closingReason,
        String guidanceTarget
) {
    public static ProgressJudgeResult normal() {
        return new ProgressJudgeResult(ResponseMode.NORMAL, null, null);
    }

    public static ProgressJudgeResult guided(String target) {
        return new ProgressJudgeResult(ResponseMode.GUIDED, null, target);
    }

    public static ProgressJudgeResult closing(ClosingReason reason) {
        return new ProgressJudgeResult(ResponseMode.CLOSING, reason, null);
    }

    public boolean isClosing() { return mode == ResponseMode.CLOSING; }
}
