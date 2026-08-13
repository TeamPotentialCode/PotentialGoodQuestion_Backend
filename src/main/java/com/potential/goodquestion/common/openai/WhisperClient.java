package com.potential.goodquestion.common.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
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
    private final ObjectMapper objectMapper;

    @Value("${openai.model.stt}")
    private String model;

    // 이야기 관련 키워드 힌트 — 아이 발음이 불명확해도 Whisper 인식률 향상
    private static final String STT_HINT =
            "며느리, 방귀, 시아버지, 시댁, 배나무, 배, 열매, 기왓장, 갓, 마을 사람, 친정, 장대, " +
            "참다, 창피하다, 부끄럽다, 솔직하게, 특별한 힘, 도움이 되다, 이상하게 생각하다, " +
            "배가 아파요, 방귀 나갑니다, 우수수 떨어졌습니다";

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("flac", "m4a", "mp3", "mp4", "mpeg", "mpga", "oga", "ogg", "wav", "webm");

    public String transcribe(MultipartFile audioFile) {
        try {
            byte[] audioBytes = audioFile.getBytes();
            String filename = resolveFilename(audioFile);

            var multipart = new LinkedMultiValueMap<String, Object>();
            multipart.add("file", new ByteArrayResource(audioBytes) {
                @Override public String getFilename() { return filename; }
            });
            multipart.add("model", model);
            multipart.add("language", "ko");
            multipart.add("prompt", STT_HINT);

            String responseJson = openAiRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(responseJson).get("text").asText();
        } catch (IOException e) {
            throw new CustomException(AiErrorCode.STT_FAILED);
        }
    }

    private String resolveFilename(MultipartFile audioFile) {
        String original = audioFile.getOriginalFilename();
        if (original != null && !original.isBlank()) {
            String ext = original.contains(".")
                    ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            if (SUPPORTED_EXTENSIONS.contains(ext)) return original;
        }
        // Content-Type으로 확장자 추론
        String contentType = audioFile.getContentType();
        if (contentType != null) {
            if (contentType.contains("webm")) return "audio.webm";
            if (contentType.contains("mp4")) return "audio.mp4";
            if (contentType.contains("m4a") || contentType.contains("mp4a")) return "audio.m4a";
            if (contentType.contains("ogg")) return "audio.ogg";
            if (contentType.contains("wav")) return "audio.wav";
            if (contentType.contains("mpeg") || contentType.contains("mp3")) return "audio.mp3";
        }
        return "audio.webm"; // 기본값 (태블릿 녹음 포맷)
    }
}
