package com.potential.goodquestion.domain.child.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.child.dto.ChildRequestDto;
import com.potential.goodquestion.domain.child.dto.ChildResponseDto;
import com.potential.goodquestion.domain.child.service.ChildService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아이 프로필 컨트롤러
 *
 * GET    /api/children          : 로그인 보호자의 아이 목록 조회
 * POST   /api/children          : 아이 프로필 등록
 * PATCH  /api/children/{childId}: 아이 프로필 수정 (이름, 나이)
 *
 * 모든 엔드포인트는 JWT 인증 필수
 * @AuthenticationPrincipal 로 SecurityContext의 CustomUserPrincipal 주입
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children")
public class ChildController {

    private final ChildService childService;

    /**
     * 아이 목록 조회
     * GET /api/children
     *
     * @param principal JWT 인증된 보호자 정보
     * @return 아이 프로필 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildResponseDto.ChildInfo>>> getChildren(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        List<ChildResponseDto.ChildInfo> children = childService.getChildren(principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("아이 목록을 조회했습니다.", children));
    }

    /**
     * 아이 프로필 등록
     * POST /api/children
     *
     * @param principal JWT 인증된 보호자 정보
     * @param request   아이 이름, 나이
     * @return 등록된 아이 프로필
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChildResponseDto.ChildInfo>> createChild(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ChildRequestDto.Create request) {

        ChildResponseDto.ChildInfo child = childService.createChild(principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("아이 프로필이 등록되었습니다.", child));
    }

    /**
     * 아이 프로필 수정 (이름, 나이)
     * PATCH /api/children/{childId}
     *
     * @param principal JWT 인증된 보호자 정보 (소유권 검증)
     * @param childId   수정 대상 아이 ID
     * @param request   변경할 이름, 나이
     * @return 수정된 아이 프로필
     */
    @PatchMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildResponseDto.ChildInfo>> updateChild(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long childId,
            @Valid @RequestBody ChildRequestDto.Update request) {

        ChildResponseDto.ChildInfo child = childService.updateChild(principal.getParentId(), childId, request);
        return ResponseEntity.ok(ApiResponse.success("아이 프로필이 수정되었습니다.", child));
    }
}

