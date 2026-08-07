package com.potential.goodquestion.common.security;

import com.potential.goodquestion.domain.parent.entity.Parent;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security 인증 컨텍스트에 저장되는 커스텀 사용자 Principal 클래스
 *
 * 필요한 이유:
 * - Spring Security 기본 User 클래스는 추가 정보(parentId 등)를 담기 어려움
 * - JwtFilter에서 인증 완료 후 SecurityContext에 저장되며,
 *   Controller에서 @AuthenticationPrincipal로 꺼내 쓸 수 있음
 *
 * 구현 인터페이스:
 * - UserDetails: Spring Security 사용자 정보 표준 인터페이스
 * - Principal: Java Security 주체 인터페이스
 *
 * 사용 위치:
 * - CustomUserDetailsService → 반환값
 * - JwtFilter → SecurityContext에 저장
 * - Controller → @AuthenticationPrincipal CustomUserPrincipal principal
 */
@Getter
@RequiredArgsConstructor
public class CustomUserPrincipal implements UserDetails, Principal {

    /** 인증된 보호자 엔티티 */
    private final Parent parent;

    /**
     * 권한 정보 반환
     * 보호자는 ROLE_USER 단일 권한 사용
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /** 비밀번호 반환 (소셜 로그인 계정은 null) */
    @Override
    public String getPassword() {
        return parent.getPassword();
    }

    /** Spring Security 식별자로 사용할 값 — parentId를 문자열로 반환 */
    @Override
    public String getUsername() {
        return String.valueOf(parent.getId());
    }

    /** Principal 인터페이스 getName() — 이메일 반환 */
    @Override
    public String getName() {
        return parent.getEmail();
    }

    /** 보호자 ID 반환 (Controller에서 편리하게 사용) */
    public Long getParentId() {
        return parent.getId();
    }

    // 아래 4개는 계정 상태 관련 메서드 — 현재는 모두 활성화 상태로 고정
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
