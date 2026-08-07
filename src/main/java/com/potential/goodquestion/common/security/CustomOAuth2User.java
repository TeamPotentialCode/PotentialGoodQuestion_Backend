package com.potential.goodquestion.common.security;

import com.potential.goodquestion.domain.parent.entity.Parent;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * OAuth2 소셜 로그인 사용자 정보를 담는 커스텀 클래스
 *
 * 필요한 이유:
 * - Spring Security 기본 DefaultOAuth2User는 Parent 엔티티 참조를 갖지 못함
 * - OAuth2SuccessHandler에서 토큰 발급 시 parentId가 필요하기 때문에
 *   Parent 엔티티를 직접 보관하는 이 클래스를 사용
 *
 * 사용 위치:
 * - CustomOAuth2UserService.loadUser() → 반환값으로 사용
 * - OAuth2SuccessHandler.onAuthenticationSuccess() → Principal 캐스팅
 */
@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    /** 인증된 보호자 엔티티 */
    private final Parent parent;

    /** OAuth2 제공자로부터 받은 원본 사용자 정보 Map */
    private final Map<String, Object> attributes;

    /** Spring Security OAuth2는 getName()이 비어있으면 안 됨 — 이메일을 식별자로 사용 */
    @Override
    public String getName() {
        return parent.getEmail();
    }

    /** 사용자 권한: 보호자는 ROLE_USER */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 보호자 ID 반환 (JWT 토큰 생성 시 사용)
     */
    public Long getParentId() {
        return parent.getId();
    }
}
