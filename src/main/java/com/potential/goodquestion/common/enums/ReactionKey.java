package com.potential.goodquestion.common.enums;

public enum ReactionKey {
    PLAYFUL_UTTERANCE,
    QUESTION_FROM_CHILD,
    PROPOSAL_FROM_CHILD,
    UNCLEAR_UTTERANCE,
    EMPATHY_FROM_CHILD,
    DISAGREEMENT,
    DIRECT_RESPONSE;

    public boolean isSoftCueSkip() {
        return this == PLAYFUL_UTTERANCE || this == QUESTION_FROM_CHILD || this == UNCLEAR_UTTERANCE;
    }
}
