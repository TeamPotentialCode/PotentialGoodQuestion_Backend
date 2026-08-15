package com.potential.goodquestion.domain.admin.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.admin.dto.AdminRequestDto;
import com.potential.goodquestion.domain.admin.dto.AdminResponseDto;
import com.potential.goodquestion.domain.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증 컨트롤러
 * 인증 없이 접근 가능한 로그인 엔드포인트만 담당
 */
@Tag(name = "Admin Auth", description = "관리자 인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminService adminService;

    @Operation(
            summary = "관리자 로그인",
            description = "관리자 코드를 입력하면 ADMIN 권한을 가진 Access Token을 발급합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminResponseDto.TokenInfo>> login(
            @Valid @RequestBody AdminRequestDto.Login request) {
        AdminResponseDto.TokenInfo token = adminService.login(request);
        return ResponseEntity.ok(ApiResponse.success("관리자 로그인이 완료되었습니다.", token));
    }
}
