package com.potential.goodquestion.engine;

import com.potential.goodquestion.common.engine.ProgressJudgeEngine;
import com.potential.goodquestion.common.engine.vo.SessionState;
import com.potential.goodquestion.common.enums.ClosingReason;
import com.potential.goodquestion.common.enums.ResponseMode;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProgressJudgeEngineTest {

    private final ProgressJudgeEngine engine = new ProgressJudgeEngine();

    @Test
    void 필수요소_충족_AND_최소턴_이상이면_CLOSING_GOAL_MET() {
        var state = new SessionState(
                3, 2, 5,
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                Set.of(),
                "NORMAL", 0, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(result.closingReason()).isEqualTo(ClosingReason.GOAL_MET);
    }

    @Test
    void maxTurns_도달시_CLOSING_MAX_TURNS() {
        var state = new SessionState(
                5, 2, 5,
                Set.of("REASON"),
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                Set.of(),
                "NORMAL", 2, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(result.closingReason()).isEqualTo(ClosingReason.MAX_TURNS);
    }

    @Test
    void 첫_발화이면_NORMAL() {
        var state = new SessionState(
                1, 2, 5,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                null, 0, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 이번_턴_신규요소_있으면_NORMAL() {
        var state = new SessionState(
                2, 2, 5,
                Set.of("REASON"),
                Set.of("REASON", "PERSPECTIVE"),
                Set.of("REASON"),
                "NORMAL", 0, 2
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 직전_GUIDED이면_NORMAL_강제() {
        var state = new SessionState(
                3, 2, 5,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "GUIDED", 2, 2
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 저정보_2회연속이면_GUIDED() {
        var state = new SessionState(
                3, 2, 5,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "NORMAL", 0, 2
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.GUIDED);
    }

    @Test
    void 신규요소없음_2회연속이면_GUIDED() {
        var state = new SessionState(
                3, 2, 5,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "NORMAL", 2, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.GUIDED);
    }

    @Test
    void 잔여턴_2이하이면_GUIDED() {
        var state = new SessionState(
                3, 2, 5,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "NORMAL", 0, 0
        );
        // maxTurns=5, turnCount=3 → remainingTurns=2 ≤ 2
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.GUIDED);
    }

    @Test
    void 조건_없으면_NORMAL() {
        var state = new SessionState(
                2, 2, 5,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "NORMAL", 0, 0
        );
        // remainingTurns=3 > 2, 저정보/신규없음 0회
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.NORMAL);
    }
}
