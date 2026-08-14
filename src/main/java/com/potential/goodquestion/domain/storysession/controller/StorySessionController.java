package com.potential.goodquestion.domain.storysession.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.storysession.dto.StorySessionRequestDto;
import com.potential.goodquestion.domain.storysession.dto.StorySessionResponseDto;
import com.potential.goodquestion.domain.storysession.service.StorySessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story Session", description = "학습 세션 API")
@RestController
@RequiredArgsConstructor
public class StorySessionController {

    private final StorySessionService storySessionService;

    @Operation(summary = "새 학습 세션 시작", description = "이야기와 아이를 선택해 새 학습 세션을 생성합니다.")
    @PostMapping("/api/stories/{storyId}/sessions")
    public ResponseEntity<ApiResponse<StorySessionResponseDto.SessionInfo>> createSession(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody StorySessionRequestDto.Create request) {
        StorySessionResponseDto.SessionInfo session =
                storySessionService.createSession(storyId, principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("학습 세션이 시작되었습니다.", session));
    }

    @Operation(
        summary = "내레이션 장면 완료",
        description = "도입·전개 내레이션 장면 재생이 끝났을 때 호출합니다. "
            + "currentScene 이 다음 장면으로 이동하며, nextCharacterName 이 null 이면 다음 장면도 내레이션, "
            + "non-null 이면 대화 장면이므로 발화 입력을 활성화하세요."
    )
    @PostMapping("/api/sessions/{sessionId}/scenes/{sceneId}/narration-complete")
    public ResponseEntity<ApiResponse<StorySessionResponseDto.NarrationResult>> completeNarration(
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @Parameter(description = "완료한 내레이션 장면 ID") @PathVariable Long sceneId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        StorySessionResponseDto.NarrationResult result =
                storySessionService.completeNarration(sessionId, sceneId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("내레이션 장면을 완료했습니다.", result));
    }

    @Operation(summary = "세션 정보 조회", description = "세션의 현재 장면, 상태 등을 반환합니다. 이어하기 복귀 시 사용하세요.")
    @GetMapping("/api/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<StorySessionResponseDto.SessionInfo>> getSession(
            @Parameter(description = "세션 ID") @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        StorySessionResponseDto.SessionInfo session =
                storySessionService.getSession(sessionId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("세션 정보를 조회했습니다.", session));
    }
}
