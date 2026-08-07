package com.potential.goodquestion.common.engine;

import com.potential.goodquestion.common.engine.vo.ProgressJudgeResult;
import com.potential.goodquestion.common.engine.vo.SessionState;
import com.potential.goodquestion.common.enums.ClosingReason;
import org.springframework.stereotype.Component;

@Component
public class ProgressJudgeEngine {

    public ProgressJudgeResult judge(SessionState state) {
        // 1. 종료 조건
        if (state.missingElements().isEmpty() && state.turnCount() >= state.preferredTurns()) {
            return ProgressJudgeResult.closing(ClosingReason.GOAL_MET);
        }
        if (state.turnCount() >= state.maxTurns()) {
            return ProgressJudgeResult.closing(ClosingReason.MAX_TURNS);
        }
        // 2. 강한 유도 제한 → NORMAL 강제
        if (state.isFirstTurn() || state.hasNewlyDetected() || state.wasGuidedLastTurn()) {
            return ProgressJudgeResult.normal();
        }
        // 3. 유도 필요성 판단
        boolean needsGuidance = !state.missingElements().isEmpty() &&
                (state.consecutiveLowInformationTurns() >= 2
                        || state.turnsWithoutNewElement() >= 2
                        || state.remainingTurns() <= 2);
        if (needsGuidance) {
            return ProgressJudgeResult.guided(null); // target은 GuidanceTargetSelector가 결정
        }
        return ProgressJudgeResult.normal();
    }
}
