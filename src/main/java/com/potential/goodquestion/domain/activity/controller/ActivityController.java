package com.potential.goodquestion.domain.activity.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.activity.dto.ActivityRequestDto;
import com.potential.goodquestion.domain.activity.dto.ActivityResponseDto;
import com.potential.goodquestion.domain.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Activity", description = "말하기 후 활동 API")
@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "후 활동 시작", description = "이야기 완료 후 카드 순서 맞추기 활동을 시작합니다. 카드가 무작위 순서로 반환됩니다.")
    @PostMapping("/api/sessions/{sessionId}/activity")
    public ResponseEntity<ApiResponse<ActivityResponseDto.CardSet>> startActivity(
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        ActivityResponseDto.CardSet cards = activityService.startActivity(sessionId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("말하기 후 활동을 시작했습니다.", cards));
    }

    @Operation(summary = "카드 순서 제출 및 재구성 저장", description = "아이가 배열한 카드 순서와 이야기 재구성 텍스트를 제출합니다.")
    @PatchMapping("/api/sessions/{sessionId}/activity")
    public ResponseEntity<ApiResponse<ActivityResponseDto.ActivityResult>> submitActivity(
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ActivityRequestDto.Submit request) {
        ActivityResponseDto.ActivityResult result =
                activityService.submitActivity(sessionId, principal.getParentId(), request);
        return ResponseEntity.ok(ApiResponse.success("말하기 후 활동을 완료했습니다.", result));
    }
}
