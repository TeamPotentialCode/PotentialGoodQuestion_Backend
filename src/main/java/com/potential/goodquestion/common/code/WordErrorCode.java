package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 단어장 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum WordErrorCode implements ErrorCode {

    // 단어 조회
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "WORD_001", "단어를 찾을 수 없습니다."),

    // 권한
    WORD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "WORD_002", "해당 단어에 접근 권한이 없습니다."),

    // 중복 저장
    WORD_ALREADY_EXISTS(HttpStatus.CONFLICT, "WORD_003", "이미 저장된 단어입니다."),

    // GPT 분석 실패
    WORD_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WORD_004", "단어 뜻·예시 문장 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
