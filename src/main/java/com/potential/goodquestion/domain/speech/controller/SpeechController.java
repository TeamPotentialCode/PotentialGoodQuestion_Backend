package com.potential.goodquestion.domain.speech.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.speech.dto.SttResponse;
import com.potential.goodquestion.domain.speech.dto.TtsRequest;
import com.potential.goodquestion.domain.speech.service.SttService;
import com.potential.goodquestion.domain.speech.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SttService sttService;
    private final TtsService ttsService;

    @Operation(summary = "음성 → 텍스트 변환 (STT)")
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SttResponse>> stt(
            @RequestParam("audio") MultipartFile audioFile) {
        return ResponseEntity.ok(ApiResponse.success(sttService.transcribe(audioFile)));
    }

    @PostMapping("/tts")
    public ResponseEntity<byte[]> tts(@RequestBody @Valid TtsRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(ttsService.synthesize(request.text()));
    }
}
