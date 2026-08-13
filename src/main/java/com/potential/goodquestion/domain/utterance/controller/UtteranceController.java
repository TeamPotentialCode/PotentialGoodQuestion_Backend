package com.potential.goodquestion.domain.utterance.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.utterance.dto.UtteranceRequest;
import com.potential.goodquestion.domain.utterance.dto.UtteranceResponse;
import com.potential.goodquestion.domain.utterance.service.UtteranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Utterance", description = "아이 발화 처리 API")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class UtteranceController {

    private final UtteranceService utteranceService;

    @Operation(
        summary = "아이 발화 제출",
        description = "아이의 발화를 저장하고 LLM으로 분석 후 캐릭터 대사를 생성합니다. sceneId는 현재 대화 중인 장면 ID입니다."
    )
    @PostMapping("/{sessionId}/utterances")
    public ResponseEntity<ApiResponse<UtteranceResponse>> processUtterance(
            @Parameter(description = "세션 ID", required = true) @PathVariable Long sessionId,
            @RequestBody @Valid UtteranceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                utteranceService.processUtterance(sessionId, request)));
    }
}
