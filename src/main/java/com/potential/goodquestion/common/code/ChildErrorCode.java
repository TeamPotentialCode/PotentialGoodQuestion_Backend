package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 아이 프로필 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum ChildErrorCode implements ErrorCode {

    // 아이 조회
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "CHILD_001", "아이 프로필을 찾을 수 없습니다."),

    // 권한
    CHILD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHILD_002", "해당 아이 프로필에 접근 권한이 없습니다."),

    // 아이 수 제한 (MVP: 계정당 1명 / 정식 서비스: 3명으로 변경 예정)
    CHILD_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CHILD_003", "등록 가능한 아이 수를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
