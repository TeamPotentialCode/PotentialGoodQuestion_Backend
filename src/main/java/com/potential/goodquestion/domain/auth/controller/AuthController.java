package com.potential.goodquestion.domain.auth.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.auth.dto.AuthRequestDto;
import com.potential.goodquestion.domain.auth.dto.AuthResponseDto;
import com.potential.goodquestion.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API (회원가입 / 로그인 / 토큰 재발급)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일, 이름, 비밀번호로 보호자 계정을 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponseDto.TokenResponse>> signup(
            @Valid @RequestBody AuthRequestDto.Signup request) {
        AuthResponseDto.TokenResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @Operation(summary = "로그인", description = "이메일, 비밀번호로 로그인하고 JWT 토큰을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto.TokenResponse>> login(
            @Valid @RequestBody AuthRequestDto.Login request) {
        AuthResponseDto.TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponseDto.TokenResponse>> refresh(
            @Valid @RequestBody AuthRequestDto.Refresh request) {
        AuthResponseDto.TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }
}
