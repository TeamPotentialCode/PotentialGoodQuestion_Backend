package com.potential.goodquestion.domain.speech.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.speech.dto.SttResponse;
import com.potential.goodquestion.domain.speech.dto.TtsRequest;
import com.potential.goodquestion.domain.speech.service.SttService;
import com.potential.goodquestion.domain.speech.service.TtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SttService sttService;
    private final TtsService ttsService;

    @PostMapping("/stt")
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
