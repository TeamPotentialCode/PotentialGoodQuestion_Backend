package com.potential.goodquestion.domain.activity.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.activity.dto.ActivityRequestDto;
import com.potential.goodquestion.domain.activity.dto.ActivityResponseDto;
import com.potential.goodquestion.domain.activity.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 말하기 후 활동 컨트롤러
 *
 * POST  /api/sessions/{sessionId}/activity : 후 활동 시작 (카드 무작위 제시)
 * PATCH /api/sessions/{sessionId}/activity : 카드 순서 제출·재구성 텍스트 저장·완료 처리
 *
 * 모든 엔드포인트 JWT 인증 필수
 */
@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 후 활동 시작 - 카드 제시
     * POST /api/sessions/{sessionId}/activity
     *
     * @param sessionId 세션 ID
     * @param principal JWT 인증된 보호자 정보
     * @return 무작위 순서로 제시할 카드 목록
     */
    @PostMapping("/api/sessions/{sessionId}/activity")
    public ResponseEntity<ApiResponse<ActivityResponseDto.CardSet>> startActivity(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        ActivityResponseDto.CardSet cards = activityService.startActivity(sessionId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("말하기 후 활동을 시작했습니다.", cards));
    }

    /**
     * 카드 순서 제출·재구성 텍스트 저장·완료 처리
     * PATCH /api/sessions/{sessionId}/activity
     *
     * @param sessionId 세션 ID
     * @param principal JWT 인증된 보호자 정보
     * @param request   제출한 카드 순서 + 재구성 텍스트
     * @return 정답 여부 및 (정답 시) 재구성 핵심 단어
     */
    @PatchMapping("/api/sessions/{sessionId}/activity")
    public ResponseEntity<ApiResponse<ActivityResponseDto.ActivityResult>> submitActivity(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ActivityRequestDto.Submit request) {

        ActivityResponseDto.ActivityResult result =
                activityService.submitActivity(sessionId, principal.getParentId(), request);
        return ResponseEntity.ok(ApiResponse.success("말하기 후 활동을 완료했습니다.", result));
    }
}
