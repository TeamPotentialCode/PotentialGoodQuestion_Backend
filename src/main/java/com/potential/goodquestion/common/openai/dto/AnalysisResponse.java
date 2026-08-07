package com.potential.goodquestion.common.openai.dto;

import com.potential.goodquestion.common.engine.vo.DetectedElement;
import java.util.List;

public record AnalysisResponse(
        String childIntent,
        List<DetectedElement> detectedElements,
        String utteranceValidity,
        String mainPoint
) {}
