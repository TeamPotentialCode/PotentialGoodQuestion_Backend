package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 회원가입
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_001", "이미 사용 중인 이메일입니다."),

    // 로그인
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "AUTH_003", "해당 이메일은 소셜 로그인으로 가입된 계정입니다."),

    // 보호자
    PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_007", "보호자 정보를 찾을 수 없습니다."),

    // 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "만료된 토큰입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_006", "Refresh Token이 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
