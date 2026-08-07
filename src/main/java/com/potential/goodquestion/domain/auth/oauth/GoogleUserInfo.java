package com.potential.goodquestion.domain.auth.oauth;

import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import java.util.Map;

/**
 * Google OAuth2 사용자 정보 파싱 클래스
 *
 * Google 응답 구조 (최상위 레벨에 정보가 바로 존재):
 * {
 *   "sub": "1234567890",        <- 구글 사용자 고유 ID
 *   "name": "홍길동",
 *   "email": "user@gmail.com",
 *   "picture": "https://..."
 * }
 */
public class GoogleUserInfo implements OAuth2UserInfo {

    /** Google API로부터 받은 원본 사용자 정보 Map */
    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /** 제공자: GOOGLE */
    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }

    /**
     * Google 사용자 고유 ID
     * Google은 "sub" 키에 사용자 ID를 담아 반환
     */
    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("sub"));
    }

    @Override
    public String getEmail() {
        return String.valueOf(attributes.get("email"));
    }

    @Override
    public String getName() {
        return String.valueOf(attributes.get("name"));
    }
}
