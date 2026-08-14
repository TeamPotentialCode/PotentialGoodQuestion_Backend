package com.potential.goodquestion.domain.speech.service;

import com.potential.goodquestion.common.openai.OpenAiTtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TtsService {

    private final OpenAiTtsClient ttsClient;

    private static final String DEFAULT_VOICE = "nova";

    // 동일 텍스트+목소리 재호출 시 캐시에서 반환
    @Cacheable(value = "tts", key = "#text + '_' + #voice")
    public byte[] synthesize(String text, String voice) {
        String resolvedVoice = (voice != null && !voice.isBlank()) ? voice : DEFAULT_VOICE;
        return ttsClient.synthesize(text, resolvedVoice);
    }
}
