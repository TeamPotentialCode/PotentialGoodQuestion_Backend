package com.potential.goodquestion.domain.speech.service;

import com.potential.goodquestion.common.openai.WhisperClient;
import com.potential.goodquestion.domain.speech.dto.SttResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SttService {

    private final WhisperClient whisperClient;

    public SttResponse transcribe(MultipartFile audioFile) {
        String text = whisperClient.transcribe(audioFile);
        return new SttResponse(text, text);
    }
}
