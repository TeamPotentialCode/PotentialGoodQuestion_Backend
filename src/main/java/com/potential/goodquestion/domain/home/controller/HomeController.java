package com.potential.goodquestion.domain.home.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.home.dto.HomeResponseDto;
import com.potential.goodquestion.domain.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 화면 조회", description = "이어하기 세션과 추천 이야기 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponseDto.HomeInfo>> getHome(
            @Parameter(description = "아이 ID", required = true) @RequestParam Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        HomeResponseDto.HomeInfo home = homeService.getHome(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("홈 화면을 조회했습니다.", home));
    }
}
