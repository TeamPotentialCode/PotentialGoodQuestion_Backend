package com.potential.goodquestion.domain.parent.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.parent.dto.ParentRequestDto;
import com.potential.goodquestion.domain.parent.dto.ParentResponseDto;
import com.potential.goodquestion.domain.parent.service.ParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*부모 컨트롤러*/
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parent")
public class ParentController {

    private final ParentService parentService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ParentResponseDto.Me>> getMe(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(parentService.getMe(principal.getParentId())));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ParentResponseDto.Me>> updateMe(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ParentRequestDto.Update request) {
        return ResponseEntity.ok(ApiResponse.success(parentService.updateMe(principal.getParentId(), request)));
    }
}
