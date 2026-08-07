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
