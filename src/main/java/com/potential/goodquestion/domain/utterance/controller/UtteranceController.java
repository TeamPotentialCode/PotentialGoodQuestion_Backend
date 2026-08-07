package com.potential.goodquestion.domain.utterance.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.utterance.dto.UtteranceRequest;
import com.potential.goodquestion.domain.utterance.dto.UtteranceResponse;
import com.potential.goodquestion.domain.utterance.service.UtteranceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class UtteranceController {

    private final UtteranceService utteranceService;

    @PostMapping("/{sessionId}/utterances")
    public ResponseEntity<ApiResponse<UtteranceResponse>> processUtterance(
            @PathVariable Long sessionId,
            @RequestBody @Valid UtteranceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                utteranceService.processUtterance(sessionId, request)));
    }
}
