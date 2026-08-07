package com.potential.goodquestion.common.engine;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GuidanceTargetSelector {

    public String select(Set<String> missingElements, String previousTarget, List<String> preferredOrder) {
        if (missingElements.isEmpty()) return null;
        return preferredOrder.stream()
                .filter(missingElements::contains)
                .filter(e -> !e.equals(previousTarget))
                .findFirst()
                .orElse(missingElements.iterator().next());
    }
}
