package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiErrorCode implements ErrorCode {

    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_001", "발화 분석에 실패했습니다."),
    CHARACTER_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_002", "캐릭터 응답 생성에 실패했습니다."),
    STT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_003", "음성 변환에 실패했습니다."),
    TTS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_004", "음성 합성에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
