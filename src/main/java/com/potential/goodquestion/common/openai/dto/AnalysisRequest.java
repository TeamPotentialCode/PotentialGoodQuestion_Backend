package com.potential.goodquestion.common.openai.dto;

import com.potential.goodquestion.common.engine.vo.DetectedElement;
import java.util.List;
import java.util.Map;

public record AnalysisRequest(
        String sceneContext,
        String goal,
        String previousCharacterMessage,
        String childUtterance,
        List<String> targetElements,
        Map<String, String> elementCriteria
) {}
