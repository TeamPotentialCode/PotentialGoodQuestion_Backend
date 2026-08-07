package com.potential.goodquestion.domain.speech.service;

import com.potential.goodquestion.common.openai.OpenAiTtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TtsService {

    private final OpenAiTtsClient ttsClient;

    public byte[] synthesize(String text) {
        return ttsClient.synthesize(text);
    }
}
