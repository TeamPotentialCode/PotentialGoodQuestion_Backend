package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 관리자 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum AdminErrorCode implements ErrorCode {

    INVALID_ADMIN_CODE(HttpStatus.UNAUTHORIZED, "ADMIN_001", "관리자 코드가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
