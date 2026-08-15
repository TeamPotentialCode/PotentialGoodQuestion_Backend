package com.potential.goodquestion.domain.child.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.child.dto.ChildRequestDto;
import com.potential.goodquestion.domain.child.dto.ChildResponseDto;
import com.potential.goodquestion.domain.child.service.ChildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Child", description = "아이 프로필 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children")
public class ChildController {

    private final ChildService childService;

    @Operation(summary = "아이 목록 조회", description = "로그인한 보호자의 아이 프로필 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildResponseDto.ChildInfo>>> getChildren(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        List<ChildResponseDto.ChildInfo> children = childService.getChildren(principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("아이 목록을 조회했습니다.", children));
    }

    @Operation(summary = "아이 프로필 단건 조회", description = "아이 ID로 프로필 하나를 반환합니다.")
    @GetMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildResponseDto.ChildInfo>> getChild(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "아이 ID") @PathVariable Long childId) {
        ChildResponseDto.ChildInfo child = childService.getChild(principal.getParentId(), childId);
        return ResponseEntity.ok(ApiResponse.success("아이 프로필을 조회했습니다.", child));
    }

    @Operation(summary = "아이 프로필 등록", description = "보호자 계정에 아이를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChildResponseDto.ChildInfo>> createChild(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ChildRequestDto.Create request) {
        ChildResponseDto.ChildInfo child = childService.createChild(principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("아이 프로필이 등록되었습니다.", child));
    }

    @Operation(summary = "아이 프로필 수정", description = "아이의 이름, 나이를 수정합니다.")
    @PatchMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildResponseDto.ChildInfo>> updateChild(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "아이 ID") @PathVariable Long childId,
            @Valid @RequestBody ChildRequestDto.Update request) {
        ChildResponseDto.ChildInfo child = childService.updateChild(principal.getParentId(), childId, request);
        return ResponseEntity.ok(ApiResponse.success("아이 프로필이 수정되었습니다.", child));
    }

    @Operation(summary = "아이 프로필 삭제",
            description = "아이 프로필과 연관 데이터(세션, 대화, 발화 분석, 활동 결과, 단어장, 동의 기록)를 함께 삭제합니다.")
    @DeleteMapping("/{childId}")
    public ResponseEntity<ApiResponse<Void>> deleteChild(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "아이 ID") @PathVariable Long childId) {
        childService.deleteChild(principal.getParentId(), childId);
        return ResponseEntity.ok(ApiResponse.success("아이 프로필이 삭제되었습니다.", null));
    }
}
