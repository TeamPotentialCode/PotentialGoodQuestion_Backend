package com.potential.goodquestion.domain.child.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.child.dto.ChildConsentRequestDto;
import com.potential.goodquestion.domain.child.dto.ChildConsentResponseDto;
import com.potential.goodquestion.domain.child.service.ChildConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Child Consent", description = "아동 개인정보 처리 동의 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children/{childId}/consent")
public class ChildConsentController {

    private final ChildConsentService childConsentService;

    @Operation(summary = "동의 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<ChildConsentResponseDto.ConsentInfo>> createConsent(
            @Parameter(description = "아이 ID") @PathVariable Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ChildConsentRequestDto.Create request) {
        ChildConsentResponseDto.ConsentInfo consent =
                childConsentService.createConsent(childId, principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("동의가 등록되었습니다.", consent));
    }

    @Operation(summary = "유효한 동의 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<ChildConsentResponseDto.ConsentInfo>> getActiveConsent(
            @Parameter(description = "아이 ID") @PathVariable Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        ChildConsentResponseDto.ConsentInfo consent =
                childConsentService.getActiveConsent(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("동의 정보를 조회했습니다.", consent));
    }

    @Operation(summary = "동의 철회")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdrawConsent(
            @Parameter(description = "아이 ID") @PathVariable Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        childConsentService.withdrawConsent(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("동의가 철회되었습니다.", null));
    }
}
