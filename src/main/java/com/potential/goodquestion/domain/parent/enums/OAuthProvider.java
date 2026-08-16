package com.potential.goodquestion.domain.parent.enums;

/**
 * 로그인 제공자
 * LOCAL: 이메일+비밀번호 일반 로그인
 * GOOGLE / NAVER: 소셜 로그인
 */
public enum OAuthProvider {
    LOCAL,
    GOOGLE,
    NAVER,
    KAKAO  // TODO: 카카오 키 발급 후 활성화
}
