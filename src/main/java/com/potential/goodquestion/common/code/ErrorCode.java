package com.potential.goodquestion.common.code;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 인터페이스
 */
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
