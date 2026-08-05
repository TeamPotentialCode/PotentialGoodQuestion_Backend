package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 성공 응답 코드
 */
@Getter
@AllArgsConstructor
public enum SuccessCode {

    OK(HttpStatus.OK, "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, "생성이 완료되었습니다.");

    private final HttpStatus status;
    private final String message;
}
