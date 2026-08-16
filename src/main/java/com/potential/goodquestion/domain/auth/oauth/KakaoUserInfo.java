package com.potential.goodquestion.domain.auth.oauth;

import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import java.util.Map;

// TODO: 카카오 키 발급 후 OAuth2UserInfoFactory, application.yaml 주석 해제

/**
 * Kakao OAuth2 사용자 정보 파싱 클래스
 *
 * Kakao 응답 구조:
 * {
 *   "id": 123456789,                          <- 카카오 사용자 고유 ID (Long)
 *   "kakao_account": {
 *     "email": "user@kakao.com",
 *     "profile": {
 *       "nickname": "홍길동"
 *     }
 *   }
 * }
 */
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile") : null;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.KAKAO;
    }

    /** 카카오 사용자 고유 ID (최상위 id 필드) */
    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getEmail() {
        if (kakaoAccount == null) return null;
        return String.valueOf(kakaoAccount.get("email"));
    }

    @Override
    public String getName() {
        if (profile == null) return null;
        return String.valueOf(profile.get("nickname"));
    }
}
