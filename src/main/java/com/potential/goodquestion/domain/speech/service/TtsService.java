package com.potential.goodquestion.domain.speech.service;

import com.potential.goodquestion.common.openai.OpenAiTtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TtsService {

    private final OpenAiTtsClient ttsClient;

    // 동일 텍스트 재호출(다시 듣기, 고정 대사 등) 시 캐시에서 반환
    @Cacheable(value = "tts", key = "#text")
    public byte[] synthesize(String text) {
        return ttsClient.synthesize(text);
    }
}
