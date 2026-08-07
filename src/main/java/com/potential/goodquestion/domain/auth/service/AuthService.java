package com.potential.goodquestion.domain.auth.service;

import com.potential.goodquestion.common.code.AuthErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.jwt.JwtUtil;
import com.potential.goodquestion.domain.auth.dto.AuthRequestDto;
import com.potential.goodquestion.domain.auth.dto.AuthResponseDto;
import com.potential.goodquestion.domain.auth.entity.Auth;
import com.potential.goodquestion.domain.auth.repository.AuthRepository;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 로그인/회원가입/토큰 재발급 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final ParentRepository parentRepository;
    private final AuthRepository authRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 일반 회원가입
     * - 이메일 중복 확인
     * - 비밀번호 암호화 저장
     * - Access/Refresh Token 발급
     */
    @Transactional
    public AuthResponseDto.TokenResponse signup(AuthRequestDto.Signup request) {
        // 이메일 중복 확인
        if (parentRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 보호자 계정 생성
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Parent parent = Parent.createLocal(request.getEmail(), request.getName(), encodedPassword);
        parentRepository.save(parent);

        // 토큰 발급 및 저장
        return issueTokens(parent);
    }

    /**
     * 일반 로그인
     * - 이메일로 계정 조회
     * - 소셜 계정이면 에러 반환
     * - 비밀번호 검증
     * - Access/Refresh Token 발급
     */
    @Transactional
    public AuthResponseDto.TokenResponse login(AuthRequestDto.Login request) {
        Parent parent = parentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_CREDENTIALS));

        // 소셜 계정 여부 확인
        if (parent.getProvider() != OAuthProvider.LOCAL) {
            throw new CustomException(AuthErrorCode.SOCIAL_ACCOUNT_EXISTS);
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), parent.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(parent);
    }

    /**
     * 토큰 재발급
     * - Refresh Token 유효성 검증
     * - DB 저장된 Refresh Token과 비교
     * - 새 Access/Refresh Token 발급 (Rotation)
     */
    @Transactional
    public AuthResponseDto.TokenResponse refresh(AuthRequestDto.Refresh request) {
        String refreshToken = request.getRefreshToken();

        // 토큰 유효성 검증
        if (!jwtUtil.validateToken(refreshToken) || !"REFRESH".equals(jwtUtil.getTokenType(refreshToken))) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }

        // DB에 저장된 Refresh Token과 비교
        Long parentId = jwtUtil.getParentId(refreshToken);
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));

        Auth auth = authRepository.findByParent(parent)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));

        if (!auth.getRefreshToken().equals(refreshToken)) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        return issueTokens(parent);
    }

    // ─────────── private ────────────────

    /**
     * Access/Refresh Token 발급 후 DB 저장 (Upsert)
     */
    private AuthResponseDto.TokenResponse issueTokens(Parent parent) {
        String accessToken = jwtUtil.generateAccessToken(parent.getId());
        String refreshToken = jwtUtil.generateRefreshToken(parent.getId());

        // Refresh Token DB 저장 (기존 있으면 갱신, 없으면 생성)
        authRepository.findByParent(parent)
                .ifPresentOrElse(
                        auth -> auth.updateRefreshToken(refreshToken),
                        () -> authRepository.save(Auth.create(parent, refreshToken))
                );

        return AuthResponseDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .parentId(parent.getId())
                .name(parent.getName())
                .build();
    }
}
