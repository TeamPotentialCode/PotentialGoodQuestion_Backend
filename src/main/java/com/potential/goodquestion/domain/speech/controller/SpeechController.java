package com.potential.goodquestion.domain.speech.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.speech.dto.SttResponse;
import com.potential.goodquestion.domain.speech.dto.TtsRequest;
import com.potential.goodquestion.domain.speech.service.SttService;
import com.potential.goodquestion.domain.speech.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Speech", description = "음성 변환 API (STT / TTS)")
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SttService sttService;
    private final TtsService ttsService;

    @Operation(
        summary = "음성 → 텍스트 변환 (STT)",
        description = "음성 파일(m4a, mp3, wav, webm 등)을 업로드하면 텍스트로 변환합니다. 아이 발화 확인 후 utterances API로 전송하세요."
    )
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SttResponse>> stt(
            @RequestParam("audio") MultipartFile audioFile) {
        return ResponseEntity.ok(ApiResponse.success(sttService.transcribe(audioFile)));
    }

    @Operation(
        summary = "텍스트 → 음성 변환 (TTS)",
        description = "텍스트를 음성(audio/mpeg)으로 변환합니다. 캐릭터 대사 또는 character_opening 재생 시 사용하세요."
    )
    @PostMapping("/tts")
    public ResponseEntity<byte[]> tts(@RequestBody @Valid TtsRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(ttsService.synthesize(request.text(), request.voice()));
    }
}
