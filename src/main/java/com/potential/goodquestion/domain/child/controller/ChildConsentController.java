package com.potential.goodquestion.domain.child.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.child.dto.ChildConsentRequestDto;
import com.potential.goodquestion.domain.child.dto.ChildConsentResponseDto;
import com.potential.goodquestion.domain.child.service.ChildConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아동 개인정보 처리 동의 컨트롤러
 *
 * POST   /api/children/{childId}/consent : 동의 등록
 * GET    /api/children/{childId}/consent : 유효한 동의 조회
 * DELETE /api/children/{childId}/consent : 동의 철회
 *
 * 모든 엔드포인트 JWT 인증 필수
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children/{childId}/consent")
public class ChildConsentController {

    private final ChildConsentService childConsentService;

    /**
     * 동의 등록
     * POST /api/children/{childId}/consent
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChildConsentResponseDto.ConsentInfo>> createConsent(
            @PathVariable Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ChildConsentRequestDto.Create request) {

        ChildConsentResponseDto.ConsentInfo consent =
                childConsentService.createConsent(childId, principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("동의가 등록되었습니다.", consent));
    }

    /**
     * 유효한 동의 조회
     * GET /api/children/{childId}/consent
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ChildConsentResponseDto.ConsentInfo>> getActiveConsent(
            @PathVariable Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        ChildConsentResponseDto.ConsentInfo consent =
                childConsentService.getActiveConsent(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("동의 정보를 조회했습니다.", consent));
    }

    /**
     * 동의 철회
     * DELETE /api/children/{childId}/consent
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdrawConsent(
            @PathVariable Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        childConsentService.withdrawConsent(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("동의가 철회되었습니다.", null));
    }
}
