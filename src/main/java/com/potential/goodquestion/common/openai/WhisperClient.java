package com.potential.goodquestion.common.openai;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import com.potential.goodquestion.common.code.AiErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class WhisperClient {

    private final RestClient openAiRestClient;

    @Value("${openai.model.stt}")
    private String model;

    // 이야기 관련 키워드 힌트 — 아이 발음이 불명확해도 Whisper 인식률 향상
    private static final String STT_HINT =
            "며느리, 방귀, 시아버지, 배나무, 시댁, 참다, 배가 아파요, 창피해요, 가족, 마을 사람, 배, 열매, 바람";

    public String transcribe(MultipartFile audioFile) {
        try {
            byte[] audioBytes = audioFile.getBytes();
            String filename = audioFile.getOriginalFilename() != null
                    ? audioFile.getOriginalFilename() : "audio.webm";

            var multipart = new LinkedMultiValueMap<String, Object>();
            multipart.add("file", new ByteArrayResource(audioBytes) {
                @Override public String getFilename() { return filename; }
            });
            multipart.add("model", model);
            multipart.add("language", "ko");
            multipart.add("prompt", STT_HINT);

            return openAiRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(JsonNode.class)
                    .get("text")
                    .asText();
        } catch (IOException e) {
            throw new CustomException(AiErrorCode.STT_FAILED);
        }
    }
}
