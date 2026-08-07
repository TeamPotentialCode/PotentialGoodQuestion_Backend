package com.potential.goodquestion.domain.auth.oauth;

import com.potential.goodquestion.domain.parent.enums.OAuthProvider;

/**
 * OAuth2 제공자별 사용자 정보를 추상화하는 인터페이스
 *
 * 역할:
 * - Google, Naver의 사용자 정보 응답 형식이 서로 다르기 때문에
 *   이 인터페이스로 통일된 접근 방식을 제공
 * - 새로운 소셜 로그인 제공자 추가 시 이 인터페이스를 구현하면 됨
 *
 * 구현체:
 * - GoogleUserInfo: Google OAuth2 응답 파싱
 * - NaverUserInfo : Naver OAuth2 응답 파싱
 */
public interface OAuth2UserInfo {

    /**
     * 소셜 로그인 제공자 종류 (GOOGLE, NAVER)
     */
    OAuthProvider getProvider();

    /**
     * 소셜 플랫폼 내 사용자 고유 ID
     * Google: sub 필드 / Naver: response.id 필드
     */
    String getProviderId();

    /**
     * 사용자 이메일
     */
    String getEmail();

    /**
     * 사용자 이름
     */
    String getName();
}
