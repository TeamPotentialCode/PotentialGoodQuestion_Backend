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
            "며느리, 방귀, 시아버지, 시댁, 배나무, 배, 열매, 기왓장, 갓, 마을 사람, 친정, 장대, " +
            "참다, 창피하다, 부끄럽다, 솔직하게, 특별한 힘, 도움이 되다, 이상하게 생각하다, " +
            "배가 아파요, 방귀 나갑니다, 우수수 떨어졌습니다";

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
