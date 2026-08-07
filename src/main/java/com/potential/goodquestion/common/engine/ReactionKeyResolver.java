package com.potential.goodquestion.common.engine;

import com.potential.goodquestion.common.engine.vo.DetectedElement;
import com.potential.goodquestion.common.enums.ReactionKey;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReactionKeyResolver {

    public ReactionKey resolve(String childIntent, String utteranceValidity, List<DetectedElement> detectedElements) {
        if ("PLAYFUL".equals(utteranceValidity) || "PLAYFUL".equals(childIntent) || "OFF_TOPIC".equals(childIntent)) {
            return ReactionKey.PLAYFUL_UTTERANCE;
        }
        if ("QUESTION".equals(childIntent)) {
            return ReactionKey.QUESTION_FROM_CHILD;
        }
        if ("SOLUTION".equals(childIntent) || hasElement(detectedElements, "SOLUTION")) {
            return ReactionKey.PROPOSAL_FROM_CHILD;
        }
        if ("SHORT_RESPONSE".equals(childIntent) || "UNCLEAR".equals(childIntent)
                || "SHORT".equals(utteranceValidity) || "UNCLEAR".equals(utteranceValidity)) {
            return ReactionKey.UNCLEAR_UTTERANCE;
        }
        if (hasElement(detectedElements, "EMPATHY")) {
            return ReactionKey.EMPATHY_FROM_CHILD;
        }
        if ("OPINION".equals(childIntent) || "REASONING".equals(childIntent) || "DECISION".equals(childIntent)
                || "PERSPECTIVE".equals(childIntent) || "REQUEST".equals(childIntent)
                || "CHALLENGE".equals(childIntent) || "EMOTION".equals(childIntent)) {
            return ReactionKey.DISAGREEMENT;
        }
        return ReactionKey.DIRECT_RESPONSE;
    }

    private boolean hasElement(List<DetectedElement> elements, String type) {
        return elements != null && elements.stream().anyMatch(e -> type.equals(e.type()));
    }
}
