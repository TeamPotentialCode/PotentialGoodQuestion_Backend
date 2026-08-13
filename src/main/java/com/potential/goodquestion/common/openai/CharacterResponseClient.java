package com.potential.goodquestion.common.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.openai.dto.CharacterRequest;
import com.potential.goodquestion.common.openai.dto.CharacterResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CharacterResponseClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model.character}")
    private String model;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 한국 전래동화 속 캐릭터 '%s'입니다.
            아이와 이야기 속에서 자연스럽게 대화하세요.

            반응 원칙:
            - 아이의 발화에 진심으로 반응하세요.
            - "해결 방법을 말해 봐", "왜 그랬을까?" 같은 학습 질문 형태는 사용하지 마세요.
            - 캐릭터의 감정과 상황 안에서 이야기하세요.
            - 짧고 자연스러운 한두 문장으로 답하세요.
            - 반드시 JSON 형식으로만 응답: {"text": "캐릭터 대사"}
            """;

    public CharacterResponse generate(CharacterRequest request) {
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, request.characterName());
        String userPrompt = buildUserPrompt(request);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_completion_tokens", 200
        );

        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String responseJson = openAiRestClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(String.class);
                String rawJson = objectMapper.readTree(responseJson)
                        .get("choices").get(0).get("message").get("content").asText();
                return objectMapper.readValue(rawJson, CharacterResponse.class);
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RuntimeException("캐릭터 응답 LLM 호출 실패 (2회 재시도): " + lastException.getMessage(), lastException);
    }

    private String buildUserPrompt(CharacterRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("장면 상황: ").append(req.sceneContext()).append("\n");
        sb.append("아이 발화: ").append(req.childUtterance()).append("\n");
        sb.append("반응 방식: ").append(req.reactionKey()).append("\n");
        if (req.previousCharacterMessage() != null) {
            sb.append("직전 내 대사: ").append(req.previousCharacterMessage()).append("\n");
        }
        if ("GUIDED".equals(req.responseMode()) && req.guidanceWorry() != null) {
            sb.append("(내가 아직 해소하지 못한 걱정 — 이번 반응의 핵심으로 드러낼 것: ")
                    .append(req.guidanceWorry()).append(")\n");
        } else if (req.softRemainingWorry() != null) {
            sb.append("(내가 아직 해소하지 못한 걱정 — 자연스럽게 약하게만 드러낼 것: ")
                    .append(req.softRemainingWorry()).append(")\n");
        }
        return sb.toString();
    }
}
