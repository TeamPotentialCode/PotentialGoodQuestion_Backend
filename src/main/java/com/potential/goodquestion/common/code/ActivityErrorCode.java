package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 말하기 후 활동 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum ActivityErrorCode implements ErrorCode {

    // 활동 조회
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTIVITY_001", "시작된 말하기 후 활동이 없습니다."),

    // 후 활동 설정
    POST_ACTIVITY_CONFIG_MISSING(HttpStatus.BAD_REQUEST, "ACTIVITY_002", "이야기에 말하기 후 활동 설정이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
