package com.potential.goodquestion.common.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.openai.dto.WordAnalysisRequest;
import com.potential.goodquestion.common.openai.dto.WordAnalysisResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 단어 뜻·예시 문장 생성 GPT 클라이언트
 * 아이가 저장한 단어에 대해 아이 눈높이 뜻과 예시 문장을 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WordAnalysisClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model.analysis}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            당신은 어린이(7세 기준) 단어 교육 전문가입니다.
            주어진 단어와 이야기 속 문장을 참고하여 반드시 JSON 형식으로만 응답하세요.

            규칙:
            1. meaning: 아이 나이에 맞게 쉽고 친근한 말로 뜻을 설명합니다. (1~2문장, 어려운 한자어 금지)
            2. exampleSentence: 일상에서 자연스럽게 사용할 수 있는 짧고 친근한 예시 문장 1개를 작성합니다.

            응답 형식: {"meaning":"...","exampleSentence":"..."}
            """;

    /**
     * 단어 뜻과 예시 문장을 GPT로 생성한다.
     * 실패 시 최대 2회 재시도한다.
     *
     * @param request 단어, 원문 문장, 아이 나이
     * @return GPT 생성 뜻·예시 문장
     */
    public WordAnalysisResponse analyze(WordAnalysisRequest request) {
        String userPrompt = buildUserPrompt(request);
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 300,
                "temperature", 0.5
        );

        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String responseJson = openAiRestClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(String.class);
                log.info("단어 분석 GPT 응답: {}", responseJson);
                String rawJson = objectMapper.readTree(responseJson)
                        .get("choices").get(0).get("message").get("content").asText();
                return objectMapper.readValue(rawJson, WordAnalysisResponse.class);
            } catch (Exception e) {
                log.error("단어 분석 GPT 호출 실패 (attempt {}): {}", attempt + 1, e.getMessage(), e);
                lastException = e;
            }
        }
        throw new RuntimeException("단어 분석 GPT 호출 실패 (2회 재시도): " + lastException.getMessage(), lastException);
    }

    private String buildUserPrompt(WordAnalysisRequest request) {
        return String.format("""
                단어: %s
                이야기 속 문장: %s
                아이 나이: %d세
                """,
                request.word(),
                request.contextSentence() != null ? request.contextSentence() : "없음",
                request.childAge()
        );
    }
}
