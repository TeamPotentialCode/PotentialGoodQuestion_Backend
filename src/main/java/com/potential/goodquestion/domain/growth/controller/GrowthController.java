package com.potential.goodquestion.domain.growth.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.growth.dto.GrowthResponseDto;
import com.potential.goodquestion.domain.growth.service.GrowthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Growth", description = "사고 요소 성장 현황 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/children")
public class GrowthController {

    private final GrowthService growthService;

    @Operation(
            summary = "사고 요소 성장 현황 조회",
            description = "아이의 전체 세션에 걸쳐 8가지 사고 요소(PERSPECTIVE, REASON, EMOTION, "
                    + "SOLUTION, RESULT, EMPATHY, REQUEST, DECISION)가 각각 몇 번 탐지됐는지 반환합니다. "
                    + "프론트에서 레이더 차트로 시각화하는 데 사용합니다. "
                    + "최근 5개 세션의 요소 탐지 목록도 함께 제공합니다."
    )
    @GetMapping("/{childId}/growth")
    public ResponseEntity<ApiResponse<GrowthResponseDto.GrowthInfo>> getGrowth(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "아이 ID") @PathVariable Long childId) {
        GrowthResponseDto.GrowthInfo growth = growthService.getGrowth(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("사고 요소 성장 현황을 조회했습니다.", growth));
    }
}
