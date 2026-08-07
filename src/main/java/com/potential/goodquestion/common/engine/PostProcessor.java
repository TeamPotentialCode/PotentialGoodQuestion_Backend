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
        Set<String> seenTypes = new HashSet<>();
        return rawElements.stream()
                .filter(e -> ThinkingElement.isValid(e.type()))
                .filter(e -> childUtterance.contains(e.evidence()))
                .filter(e -> seenTypes.add(e.type()))
                .collect(Collectors.toList());
    }
}
