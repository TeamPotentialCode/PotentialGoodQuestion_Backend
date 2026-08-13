package com.potential.goodquestion.domain.speech.service;

import com.potential.goodquestion.common.openai.WhisperClient;
import com.potential.goodquestion.domain.speech.dto.SttResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SttService {

    private final WhisperClient whisperClient;

    // Whisper가 무음/배경소음에서 생성하는 환각 패턴
    private static final List<Pattern> HALLUCINATION_PATTERNS = List.of(
            // 유튜브/방송 광고·저작권 문구
            Pattern.compile("유료\\s*광고"),
            Pattern.compile("이\\s*영상은.*광고"),
            Pattern.compile("구독과\\s*좋아요"),
            Pattern.compile("구독.*알림\\s*설정"),
            Pattern.compile("알림\\s*설정.*구독"),
            Pattern.compile("좋아요.*눌러"),
            Pattern.compile("시청해\\s*주셔서\\s*감사"),
            Pattern.compile("다음\\s*영상에서\\s*만나"),

            // 자막/번역 봉사 문구
            Pattern.compile("자막\\s*제공"),
            Pattern.compile("자막\\s*봉사"),
            Pattern.compile("번역\\s*봉사"),
            Pattern.compile("번역\\s*제공"),

            // 방송사명 단독 출력
            Pattern.compile("^\\s*(KBS|SBS|MBC|EBS|JTBC|tvN|OCN)\\s*$"),

            // 영어 환각 (Whisper 학습 데이터 오염)
            Pattern.compile("(?i)thank(s)? for watching"),
            Pattern.compile("(?i)please\\s+subscribe"),
            Pattern.compile("(?i)like and subscribe"),
            Pattern.compile("(?i)see you in the next"),
            Pattern.compile("(?i)don.t forget to subscribe"),
            Pattern.compile("(?i)subtitles? by"),
            Pattern.compile("(?i)translated by"),

            // 소리 태그 (Whisper가 배경음을 태그로 표현)
            Pattern.compile("[\\[\\(]\\s*(음악|Music|박수|Applause|웃음|Laughter|소음|노이즈)[\\]\\)]"),

            // 구두점·특수문자만 있는 경우
            Pattern.compile("^[\\s.,!?。、…·~\\-_=+*#@&%^()\\[\\]{}\"'`|/\\\\]+$"),

            // 빈 문자열 / 공백만
            Pattern.compile("^\\s*$"),

            // 동일 문자 5회 이상 연속 반복 (노이즈 환각)
            Pattern.compile("(.)\\1{4,}")
    );

    public SttResponse transcribe(MultipartFile audioFile) {
        String raw = whisperClient.transcribe(audioFile);
        String text = isHallucination(raw) ? "" : raw;
        return new SttResponse(text, raw);
    }

    private boolean isHallucination(String text) {
        if (text == null) return true;
        return HALLUCINATION_PATTERNS.stream()
                .anyMatch(p -> p.matcher(text).find());
    }
}
