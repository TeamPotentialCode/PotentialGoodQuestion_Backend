package com.potential.goodquestion.engine;

import com.potential.goodquestion.common.engine.ReactionKeyResolver;
import com.potential.goodquestion.common.engine.vo.DetectedElement;
import com.potential.goodquestion.common.enums.ReactionKey;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReactionKeyResolverTest {

    private final ReactionKeyResolver resolver = new ReactionKeyResolver();

    // ── PLAYFUL_UTTERANCE ────────────────────────────────────────────────────

    @Test
    void validity_PLAYFUL이면_PLAYFUL_UTTERANCE() {
        assertThat(resolver.resolve("OPINION", "PLAYFUL", List.of()))
                .isEqualTo(ReactionKey.PLAYFUL_UTTERANCE);
    }

    @Test
    void intent_PLAYFUL이면_PLAYFUL_UTTERANCE() {
        assertThat(resolver.resolve("PLAYFUL", "VALID", List.of()))
                .isEqualTo(ReactionKey.PLAYFUL_UTTERANCE);
    }

    @Test
    void intent_OFF_TOPIC이면_PLAYFUL_UTTERANCE() {
        assertThat(resolver.resolve("OFF_TOPIC", "VALID", List.of()))
                .isEqualTo(ReactionKey.PLAYFUL_UTTERANCE);
    }

    // ── QUESTION_FROM_CHILD ──────────────────────────────────────────────────

    @Test
    void intent_QUESTION이면_QUESTION_FROM_CHILD() {
        assertThat(resolver.resolve("QUESTION", "VALID", List.of()))
                .isEqualTo(ReactionKey.QUESTION_FROM_CHILD);
    }

    // ── PROPOSAL_FROM_CHILD ──────────────────────────────────────────────────

    @Test
    void intent_SOLUTION이면_PROPOSAL_FROM_CHILD() {
        assertThat(resolver.resolve("SOLUTION", "VALID", List.of()))
                .isEqualTo(ReactionKey.PROPOSAL_FROM_CHILD);
    }

    @Test
    void SOLUTION_요소_탐지되면_PROPOSAL_FROM_CHILD() {
        var elements = List.of(new DetectedElement("SOLUTION", "집에 가면 돼요"));
        assertThat(resolver.resolve("OPINION", "VALID", elements))
                .isEqualTo(ReactionKey.PROPOSAL_FROM_CHILD);
    }

    // ── UNCLEAR_UTTERANCE ────────────────────────────────────────────────────

    @Test
    void validity_SHORT이면_UNCLEAR_UTTERANCE() {
        assertThat(resolver.resolve("OPINION", "SHORT", List.of()))
                .isEqualTo(ReactionKey.UNCLEAR_UTTERANCE);
    }

    @Test
    void validity_UNCLEAR이면_UNCLEAR_UTTERANCE() {
        assertThat(resolver.resolve("OPINION", "UNCLEAR", List.of()))
                .isEqualTo(ReactionKey.UNCLEAR_UTTERANCE);
    }

    @Test
    void intent_SHORT_RESPONSE이면_UNCLEAR_UTTERANCE() {
        assertThat(resolver.resolve("SHORT_RESPONSE", "VALID", List.of()))
                .isEqualTo(ReactionKey.UNCLEAR_UTTERANCE);
    }

    @Test
    void intent_UNCLEAR이면_UNCLEAR_UTTERANCE() {
        assertThat(resolver.resolve("UNCLEAR", "VALID", List.of()))
                .isEqualTo(ReactionKey.UNCLEAR_UTTERANCE);
    }

    // ── EMPATHY_FROM_CHILD ───────────────────────────────────────────────────

    @Test
    void EMPATHY_요소_탐지되면_EMPATHY_FROM_CHILD() {
        var elements = List.of(new DetectedElement("EMPATHY", "힘들었겠다"));
        assertThat(resolver.resolve("OPINION", "VALID", elements))
                .isEqualTo(ReactionKey.EMPATHY_FROM_CHILD);
    }

    // ── DISAGREEMENT ─────────────────────────────────────────────────────────

    @Test
    void intent_REASONING이면_DISAGREEMENT() {
        assertThat(resolver.resolve("REASONING", "VALID", List.of()))
                .isEqualTo(ReactionKey.DISAGREEMENT);
    }

    @Test
    void intent_DECISION이면_DISAGREEMENT() {
        assertThat(resolver.resolve("DECISION", "VALID", List.of()))
                .isEqualTo(ReactionKey.DISAGREEMENT);
    }

    @Test
    void intent_OPINION이면_DISAGREEMENT() {
        assertThat(resolver.resolve("OPINION", "VALID", List.of()))
                .isEqualTo(ReactionKey.DISAGREEMENT);
    }

    @Test
    void intent_PERSPECTIVE이면_DISAGREEMENT() {
        assertThat(resolver.resolve("PERSPECTIVE", "VALID", List.of()))
                .isEqualTo(ReactionKey.DISAGREEMENT);
    }

    @Test
    void intent_CHALLENGE이면_DISAGREEMENT() {
        assertThat(resolver.resolve("CHALLENGE", "VALID", List.of()))
                .isEqualTo(ReactionKey.DISAGREEMENT);
    }

    // ── DIRECT_RESPONSE ──────────────────────────────────────────────────────

    @Test
    void 인식불가_intent는_DIRECT_RESPONSE() {
        assertThat(resolver.resolve(null, "VALID", List.of()))
                .isEqualTo(ReactionKey.DIRECT_RESPONSE);
    }

    // ── soft-cue skip ────────────────────────────────────────────────────────

    @Test
    void PLAYFUL_UTTERANCE는_softCue_스킵() {
        assertThat(ReactionKey.PLAYFUL_UTTERANCE.isSoftCueSkip()).isTrue();
    }

    @Test
    void QUESTION_FROM_CHILD는_softCue_스킵() {
        assertThat(ReactionKey.QUESTION_FROM_CHILD.isSoftCueSkip()).isTrue();
    }

    @Test
    void UNCLEAR_UTTERANCE는_softCue_스킵() {
        assertThat(ReactionKey.UNCLEAR_UTTERANCE.isSoftCueSkip()).isTrue();
    }

    @Test
    void PROPOSAL_FROM_CHILD는_softCue_스킵_아님() {
        assertThat(ReactionKey.PROPOSAL_FROM_CHILD.isSoftCueSkip()).isFalse();
    }

    @Test
    void DIRECT_RESPONSE는_softCue_스킵_아님() {
        assertThat(ReactionKey.DIRECT_RESPONSE.isSoftCueSkip()).isFalse();
    }
}
