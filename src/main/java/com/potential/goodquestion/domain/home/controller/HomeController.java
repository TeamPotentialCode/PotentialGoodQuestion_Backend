package com.potential.goodquestion.domain.home.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.home.dto.HomeResponseDto;
import com.potential.goodquestion.domain.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈 화면 컨트롤러
 *
 * GET /api/home?childId= : 이어하기 + 추천 이야기
 *
 * JWT 인증 필수 (childId 는 로그인 보호자 소유 아이여야 함)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    /**
     * 홈 화면 조회
     * GET /api/home?childId={childId}
     *
     * @param childId   대상 아이 ID
     * @param principal JWT 인증된 보호자 정보
     * @return 이어하기 세션 + 추천 이야기 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponseDto.HomeInfo>> getHome(
            @RequestParam Long childId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        HomeResponseDto.HomeInfo home = homeService.getHome(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("홈 화면을 조회했습니다.", home));
    }
}
