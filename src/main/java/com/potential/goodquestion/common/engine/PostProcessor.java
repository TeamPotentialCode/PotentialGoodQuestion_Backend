package com.potential.goodquestion.common.engine;

import com.potential.goodquestion.common.engine.vo.DetectedElement;
import com.potential.goodquestion.common.enums.ThinkingElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PostProcessor {

    public List<DetectedElement> process(List<DetectedElement> rawElements, String childUtterance) {
        String normalizedUtterance = normalize(childUtterance);
        Set<String> seenTypes = new HashSet<>();
        return rawElements.stream()
                .filter(e -> ThinkingElement.isValid(e.type()))
                .filter(e -> e.evidence() != null && !e.evidence().isBlank())
                .filter(e -> containsEvidence(normalizedUtterance, childUtterance, e.evidence()))
                .filter(e -> seenTypes.add(e.type()))
                .collect(Collectors.toList());
    }

    private boolean containsEvidence(String normalizedUtterance, String originalUtterance, String evidence) {
        // 원문 exact match 우선
        if (originalUtterance.contains(evidence)) return true;
        // 공백·구두점 정규화 후 재시도
        return normalizedUtterance.contains(normalize(evidence));
    }

    private String normalize(String text) {
        return text.replaceAll("[\\s,.!?。、]+", " ").strip();
    }
}
