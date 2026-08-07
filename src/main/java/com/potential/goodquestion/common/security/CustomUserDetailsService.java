package com.potential.goodquestion.common.security;

import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security에서 사용자 정보를 조회하는 커스텀 서비스
 *
 * 역할:
 * - UserDetailsService 인터페이스 구현 (Spring Security 표준)
 * - 이메일로 조회: 일반 로그인 시 AuthenticationManager가 호출
 * - ID로 조회: JWT 필터에서 토큰 검증 후 호출
 *
 * 사용 흐름:
 * [일반 로그인] AuthService.login() → passwordEncoder.matches() → CustomUserPrincipal 반환
 * [JWT 인증]   JwtFilter → loadUserById() → SecurityContext 저장 → Controller
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final ParentRepository parentRepository;

    /**
     * 이메일로 보호자 조회 (Spring Security 표준 메서드 — 일반 로그인 시 사용)
     *
     * @param email 보호자 이메일
     * @return CustomUserPrincipal (Spring Security 인증 컨텍스트에 저장됨)
     * @throws UsernameNotFoundException 해당 이메일의 계정이 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Parent parent = parentRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("이메일에 해당하는 사용자를 찾을 수 없습니다: " + email));
        return new CustomUserPrincipal(parent);
    }

    /**
     * 보호자 ID로 조회 (JWT 필터에서 토큰 검증 후 사용)
     *
     * @param id 보호자 ID (JWT subject 필드)
     * @return CustomUserPrincipal
     * @throws UsernameNotFoundException 해당 ID의 계정이 없을 때
     */
    @Transactional(readOnly = true)
    public CustomUserPrincipal loadUserById(Long id) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("ID에 해당하는 사용자를 찾을 수 없습니다: " + id));
        return new CustomUserPrincipal(parent);
    }
}
