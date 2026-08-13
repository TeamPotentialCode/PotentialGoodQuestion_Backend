package com.potential.goodquestion.common.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.openai.dto.AnalysisRequest;
import com.potential.goodquestion.common.openai.dto.AnalysisResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisLlmClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model.analysis}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            당신은 아이의 발화를 분석하는 전문가입니다. 제공된 장면 맥락을 바탕으로 아이의 발화를 분석하고 반드시 JSON 형식으로만 응답하세요.

            분석 규칙:
            1. childIntent: 발화의 중심 의도 (QUESTION/OPINION/REASONING/SOLUTION/DECISION/PERSPECTIVE/EMOTION/REQUEST/CHALLENGE/PLAYFUL/OFF_TOPIC/SHORT_RESPONSE/UNCLEAR 중 하나)
            2. detectedElements: 이번 발화에서 직접 확인된 사고 요소와 원문 근거. evidence는 반드시 childUtterance의 부분 문자열이어야 함.
            3. utteranceValidity: VALID/SHORT/UNCLEAR/OFF_TOPIC/PLAYFUL 중 하나
            4. mainPoint: 발화의 핵심 의미 (없으면 null)

            절대 금지: 아이가 말하지 않은 내용을 추론하거나 evidence를 만들어내지 마세요.
            응답 형식: {"childIntent":"REASONING","detectedElements":[{"type":"PERSPECTIVE","evidence":"창피해서 계속 참았던 것 같아요"}],"utteranceValidity":"VALID","mainPoint":"며느리가 수치심으로 인해 방귀를 참았다는 것"}
            """;

    public AnalysisResponse analyze(AnalysisRequest request) {
        String userPrompt = buildUserPrompt(request);
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 500,
                "temperature", 0.3
        );

        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String responseJson = openAiRestClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(String.class);
                log.info("OpenAI 응답 원문: {}", responseJson);
                String rawJson = objectMapper.readTree(responseJson)
                        .get("choices").get(0).get("message").get("content").asText();
                return objectMapper.readValue(rawJson, AnalysisResponse.class);
            } catch (Exception e) {
                log.error("발화 분석 LLM 호출 실패 (attempt {}): {}", attempt + 1, e.getMessage(), e);
                lastException = e;
            }
        }
        throw new RuntimeException("발화 분석 LLM 호출 실패 (2회 재시도): " + lastException.getMessage(), lastException);
    }

    private String buildUserPrompt(AnalysisRequest req) {
        String criteriaJson;
        try {
            criteriaJson = objectMapper.writeValueAsString(req.elementCriteria());
        } catch (Exception e) {
            criteriaJson = req.elementCriteria().toString();
        }
        return String.format("""
                장면 상황: %s
                학습 목표: %s
                직전 캐릭터 대사: %s
                아이 발화: %s
                확인할 사고 요소: %s
                요소별 인정 기준: %s
                """,
                req.sceneContext(),
                req.goal(),
                req.previousCharacterMessage() != null ? req.previousCharacterMessage() : "없음",
                req.childUtterance(),
                req.targetElements(),
                criteriaJson
        );
    }
}
