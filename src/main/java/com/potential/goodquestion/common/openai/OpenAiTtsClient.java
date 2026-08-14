package com.potential.goodquestion.common.openai;

import com.potential.goodquestion.common.code.AiErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenAiTtsClient {

    private final RestClient openAiRestClient;

    @Value("${openai.model.tts}")
    private String model;

    @Value("${openai.model.tts-voice}")
    private String voice;

    public byte[] synthesize(String text, String voice) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text,
                    "voice", voice
            );
            return openAiRestClient.post()
                    .uri("/audio/speech")
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            throw new CustomException(AiErrorCode.TTS_FAILED);
        }
    }
}
