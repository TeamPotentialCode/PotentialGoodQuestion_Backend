package com.potential.goodquestion.domain.auth.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.auth.dto.AuthRequestDto;
import com.potential.goodquestion.domain.auth.dto.AuthResponseDto;
import com.potential.goodquestion.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 컨트롤러
 * POST /api/auth/signup  - 일반 회원가입
 * POST /api/auth/login   - 일반 로그인
 * POST /api/auth/refresh - 토큰 재발급
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 일반 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponseDto.TokenResponse>> signup(
            @Valid @RequestBody AuthRequestDto.Signup request) {
        AuthResponseDto.TokenResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto.TokenResponse>> login(
            @Valid @RequestBody AuthRequestDto.Login request) {
        AuthResponseDto.TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    /**
     * 토큰 재발급 (Refresh Token Rotation)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDto.TokenResponse>> refresh(
            @Valid @RequestBody AuthRequestDto.Refresh request) {
        AuthResponseDto.TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }
}
