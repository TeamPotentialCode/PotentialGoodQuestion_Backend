package com.potential.goodquestion.domain.auth.oauth;

import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import java.util.Map;

/**
 * Naver OAuth2 사용자 정보 파싱 클래스
 *
 * Naver 응답 구조 (최상위가 아닌 "response" 키 안에 사용자 정보가 존재):
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "32742776",          <- 네이버 사용자 고유 ID
 *     "name": "홍길동",
 *     "email": "user@naver.com",
 *     "nickname": "닉네임",
 *     "profile_image": "https://..."
 *   }
 * }
 *
 * Google과 달리 "response" 키 안을 한 번 더 파싱해야 함에 주의
 */
public class NaverUserInfo implements OAuth2UserInfo {

    /** Naver API 원본 응답 Map (최상위) */
    private final Map<String, Object> attributes;

    /** "response" 키 내부의 실제 사용자 정보 Map */
    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        // 네이버는 최상위 "response" 키 안에 실제 사용자 정보가 들어있음
        this.response = (Map<String, Object>) attributes.get("response");
    }

    /** 제공자: NAVER */
    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.NAVER;
    }

    /**
     * Naver 사용자 고유 ID
     * response.id 필드에 존재
     */
    @Override
    public String getProviderId() {
        if (response == null) return null;
        return String.valueOf(response.get("id"));
    }

    @Override
    public String getEmail() {
        if (response == null) return null;
        return String.valueOf(response.get("email"));
    }

    @Override
    public String getName() {
        if (response == null) return null;
        return String.valueOf(response.get("name"));
    }
}
