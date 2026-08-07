package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 세션 관련 에러 코드
 *
 * 전우선 담당 UtteranceService에서도 사용 (SESSION_001)
 */
@Getter
@AllArgsConstructor
public enum SessionErrorCode implements ErrorCode {

    // 세션 조회
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_001", "진행 중인 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "SESSION_002", "이미 완료된 세션입니다."),

    // 세션 생성
    STORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_003", "이야기를 찾을 수 없습니다."),
    CHILD_NOT_FOUND_FOR_SESSION(HttpStatus.NOT_FOUND, "SESSION_004", "아이 프로필을 찾을 수 없습니다."),
    SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SESSION_005", "해당 세션에 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
