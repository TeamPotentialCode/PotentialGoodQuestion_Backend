package com.potential.goodquestion.domain.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.jwt.JwtUtil;
import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomOAuth2User;
import com.potential.goodquestion.domain.auth.dto.AuthResponseDto;
import com.potential.goodquestion.domain.auth.entity.Auth;
import com.potential.goodquestion.domain.auth.repository.AuthRepository;
import com.potential.goodquestion.domain.parent.entity.Parent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 소셜 로그인 성공 핸들러
 *
 * 역할:
 * 1. CustomOAuth2User에서 Parent 엔티티 추출 (이미 DB에 저장된 상태)
 * 2. Access Token + Refresh Token 발급
 * 3. Refresh Token DB 저장 (auth_tokens 테이블, Upsert)
 * 4. 토큰을 JSON 응답으로 반환
 *
 * 실행 시점:
 * CustomOAuth2UserService.loadUser() 완료 → Spring Security 인증 성공
 * → OAuth2SuccessHandler.onAuthenticationSuccess() 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final AuthRepository authRepository;
    private final ObjectMapper objectMapper;

    /**
     * 소셜 로그인 성공 시 JWT 토큰 발급 후 JSON 응답 반환
     *
     * @param authentication CustomOAuth2User를 Principal로 갖는 인증 객체
     */
    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // CustomOAuth2UserService에서 이미 저장된 Parent 엔티티를 꺼냄
        // (이메일로 다시 DB 조회하는 방식보다 효율적)
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Parent parent = oAuth2User.getParent();

        log.info("소셜 로그인 성공: provider={}, parentId={}", parent.getProvider(), parent.getId());

        // Access Token + Refresh Token 발급
        String accessToken = jwtUtil.generateAccessToken(parent.getId());
        String refreshToken = jwtUtil.generateRefreshToken(parent.getId());

        // Refresh Token DB 저장 (기존 있으면 갱신, 없으면 신규 생성)
        authRepository.findByParent(parent)
                .ifPresentOrElse(
                        auth -> auth.updateRefreshToken(refreshToken),
                        () -> authRepository.save(Auth.create(parent, refreshToken))
                );

        // JSON 응답 반환
        AuthResponseDto.TokenResponse tokenResponse = AuthResponseDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .parentId(parent.getId())
                .name(parent.getName())
                .build();

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.success("소셜 로그인이 완료되었습니다.", tokenResponse))
        );
    }
}
