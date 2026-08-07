package com.potential.goodquestion.domain.auth.oauth;

import java.util.Map;

/**
 * OAuth2 제공자에 따라 적절한 OAuth2UserInfo 구현체를 생성하는 팩토리 클래스
 *
 * 역할:
 * - registrationId(google, naver)를 보고 알맞은 파싱 클래스를 반환
 * - 새로운 소셜 로그인 제공자 추가 시 이 팩토리에만 case를 추가하면 됨
 *
 * 사용 위치:
 * - CustomOAuth2UserService.loadUser() 에서 호출
 */
public class OAuth2UserInfoFactory {

    /**
     * registrationId에 맞는 OAuth2UserInfo 구현체 반환
     *
     * @param registrationId application.yaml에 등록된 소셜 로그인 키 (google, naver)
     * @param attributes     OAuth2 제공자로부터 받은 원본 사용자 정보 Map
     * @return 제공자별 파싱 구현체
     */
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleUserInfo(attributes);
            case "naver"  -> new NaverUserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        };
    }
}
