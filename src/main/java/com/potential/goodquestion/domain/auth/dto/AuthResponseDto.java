package com.potential.goodquestion.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 응답 DTO 모음
 */
public class AuthResponseDto {

    /**
     * 로그인 / 회원가입 성공 응답 (토큰 반환)
     */
    @Getter
    @Builder
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private Long parentId;
        private String name;
    }
}
