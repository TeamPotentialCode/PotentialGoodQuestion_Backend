package com.potential.goodquestion.domain.storysession.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.storysession.dto.StorySessionRequestDto;
import com.potential.goodquestion.domain.storysession.dto.StorySessionResponseDto;
import com.potential.goodquestion.domain.storysession.service.StorySessionService;
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

/**
 * 스토리 세션 컨트롤러
 *
 * POST /api/stories/{storyId}/sessions : 새 학습 세션 시작
 * GET  /api/sessions/{sessionId}       : 세션 정보 조회 (이어하기 복귀)
 *
 * 모든 엔드포인트 JWT 인증 필수
 * @AuthenticationPrincipal 로 SecurityContext의 CustomUserPrincipal 주입
 */
@RestController
@RequiredArgsConstructor
public class StorySessionController {

    private final StorySessionService storySessionService;

    /**
     * 새 학습 세션 시작
     * POST /api/stories/{storyId}/sessions
     *
     * @param storyId   이야기 ID
     * @param principal JWT 인증된 보호자 정보
     * @param request   학습할 아이 ID (childId)
     * @return 생성된 세션 정보
     */
    @PostMapping("/api/stories/{storyId}/sessions")
    public ResponseEntity<ApiResponse<StorySessionResponseDto.SessionInfo>> createSession(
            @PathVariable Long storyId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody StorySessionRequestDto.Create request) {

        StorySessionResponseDto.SessionInfo session =
                storySessionService.createSession(storyId, principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("학습 세션이 시작되었습니다.", session));
    }

    /**
     * 세션 정보 조회 (이어하기 복귀)
     * GET /api/sessions/{sessionId}
     *
     * @param sessionId 세션 ID
     * @param principal JWT 인증된 보호자 정보 (소유권 검증)
     * @return 세션 정보 (현재 장면 ID, 상태 등)
     */
    @GetMapping("/api/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<StorySessionResponseDto.SessionInfo>> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        StorySessionResponseDto.SessionInfo session =
                storySessionService.getSession(sessionId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("세션 정보를 조회했습니다.", session));
    }
}
