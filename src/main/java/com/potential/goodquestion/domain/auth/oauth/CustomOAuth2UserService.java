package com.potential.goodquestion.domain.auth.oauth;

import com.potential.goodquestion.common.security.CustomOAuth2User;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 소셜 로그인 사용자 정보 처리 서비스
 *
 * 역할:
 * 1. OAuth2 제공자(Google, Naver)로부터 사용자 정보 가져오기
 * 2. OAuth2UserInfoFactory로 제공자별 응답 형식 통일
 * 3. DB에 없으면 자동 회원가입, 있으면 이름 업데이트
 * 4. CustomOAuth2User 반환 → OAuth2SuccessHandler에서 토큰 발급에 사용
 *
 * 실행 시점:
 * 소셜 로그인 콜백 → Spring Security → loadUser() → saveOrUpdate() → CustomOAuth2User 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final ParentRepository parentRepository;

    /**
     * OAuth2 제공자로부터 사용자 정보를 로드하고 DB에 저장/업데이트
     *
     * @param userRequest OAuth2 인증 요청 정보 (제공자, 토큰 등)
     * @return CustomOAuth2User (Parent 엔티티 + 원본 attributes 포함)
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Spring Security 기본 구현체로 소셜 플랫폼에서 사용자 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 어떤 소셜 플랫폼인지 확인 (google / naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 시도: provider={}", registrationId);

        // 3. OAuth2UserInfoFactory로 제공자별 응답 형식 통일
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        // 4. DB에 없으면 자동 회원가입, 있으면 이름 업데이트
        Parent parent = saveOrUpdate(userInfo);

        // 5. provider access token 추출 — 탈퇴 시 unlink 호출에 사용
        String oauthAccessToken = userRequest.getAccessToken().getTokenValue();

        // 6. Parent + 원본 attributes + provider token을 담은 CustomOAuth2User 반환
        //    → OAuth2SuccessHandler에서 parent.getId()로 JWT 발급
        return new CustomOAuth2User(parent, oAuth2User.getAttributes(), oauthAccessToken);
    }

    /**
     * 소셜 로그인 보호자 계정 저장 또는 업데이트
     * - 신규 사용자: 자동 회원가입 (이메일, 이름, 제공자 정보 저장)
     * - 기존 사용자: 이름 업데이트
     *
     * @param userInfo 제공자별 파싱된 사용자 정보
     * @return 저장된 Parent 엔티티
     */
    private Parent saveOrUpdate(OAuth2UserInfo userInfo) {
        return parentRepository.findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .map(parent -> {
                    // 기존 계정 — 이름 업데이트
                    log.info("기존 소셜 로그인 사용자: provider={}, email={}", userInfo.getProvider(), userInfo.getEmail());
                    parent.updateName(userInfo.getName());
                    return parent;
                })
                .orElseGet(() -> {
                    // 신규 계정 — 자동 회원가입
                    log.info("신규 소셜 로그인 사용자: provider={}, email={}", userInfo.getProvider(), userInfo.getEmail());
                    return parentRepository.save(
                            Parent.createOAuth(
                                    userInfo.getEmail(),
                                    userInfo.getName(),
                                    userInfo.getProvider(),
                                    userInfo.getProviderId()
                            )
                    );
                });
    }
}
