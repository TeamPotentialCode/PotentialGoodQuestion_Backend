package com.potential.goodquestion.domain.parent.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.parent.dto.ParentRequestDto;
import com.potential.goodquestion.domain.parent.dto.ParentResponseDto;
import com.potential.goodquestion.domain.parent.service.ParentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Operation(summary = "회원 탈퇴",
            description = "보호자 계정과 연관 데이터(아이 프로필, 세션, 대화, 발화 분석, 활동 결과, 단어장, 동의 기록, 토큰)를 모두 삭제합니다. 되돌릴 수 없습니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        parentService.withdraw(principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }
}
