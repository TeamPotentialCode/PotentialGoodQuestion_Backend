package com.potential.goodquestion.domain.admin.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.admin.dto.AdminRequestDto;
import com.potential.goodquestion.domain.admin.dto.AdminResponseDto;
import com.potential.goodquestion.domain.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 컨트롤러
 * 모든 엔드포인트는 ROLE_ADMIN 권한 필요 (SecurityConfig에서 제어)
 */
@Tag(name = "Admin", description = "관리자 전용 API (ADMIN 토큰 필요)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    // ═══════════════════════════════════════════
    // 대시보드
    // ═══════════════════════════════════════════

    @Operation(
            summary = "전체 현황 대시보드",
            description = "총 보호자 수, 오늘 신규 가입, 세션 현황, 이야기 수를 반환합니다."
    )
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminResponseDto.DashboardInfo>> getDashboard() {
        AdminResponseDto.DashboardInfo dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("대시보드를 조회했습니다.", dashboard));
    }

    // ═══════════════════════════════════════════
    // 회원 관리
    // ═══════════════════════════════════════════

    @Operation(
            summary = "회원 목록 조회",
            description = "전체 보호자 목록을 최근 가입순으로 반환합니다. (페이지 지원)"
    )
    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<Page<AdminResponseDto.ParentSummary>>> getParents(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<AdminResponseDto.ParentSummary> parents = adminService.getParents(pageable);
        return ResponseEntity.ok(ApiResponse.success("회원 목록을 조회했습니다.", parents));
    }

    @Operation(
            summary = "회원 상세 조회",
            description = "보호자 정보와 등록된 아이 목록을 함께 반환합니다."
    )
    @GetMapping("/parents/{parentId}")
    public ResponseEntity<ApiResponse<AdminResponseDto.ParentDetail>> getParentDetail(
            @Parameter(description = "보호자 ID") @PathVariable Long parentId) {
        AdminResponseDto.ParentDetail detail = adminService.getParentDetail(parentId);
        return ResponseEntity.ok(ApiResponse.success("회원 상세를 조회했습니다.", detail));
    }

    @Operation(
            summary = "회원의 아이 목록 조회",
            description = "특정 보호자에 등록된 아이 목록을 반환합니다."
    )
    @GetMapping("/parents/{parentId}/children")
    public ResponseEntity<ApiResponse<List<AdminResponseDto.ChildSummary>>> getChildrenByParent(
            @Parameter(description = "보호자 ID") @PathVariable Long parentId) {
        List<AdminResponseDto.ChildSummary> children = adminService.getChildrenByParent(parentId);
        return ResponseEntity.ok(ApiResponse.success("아이 목록을 조회했습니다.", children));
    }

    // ═══════════════════════════════════════════
    // 세션 관리
    // ═══════════════════════════════════════════

    @Operation(
            summary = "아이 이야기 진척도 조회",
            description = "특정 아이의 세션 목록과 각 세션의 진척 현황을 최근 활동순으로 반환합니다."
    )
    @GetMapping("/children/{childId}/sessions")
    public ResponseEntity<ApiResponse<List<AdminResponseDto.SessionProgress>>> getChildSessions(
            @Parameter(description = "아이 ID") @PathVariable Long childId) {
        List<AdminResponseDto.SessionProgress> sessions = adminService.getChildSessions(childId);
        return ResponseEntity.ok(ApiResponse.success("아이 이야기 진척도를 조회했습니다.", sessions));
    }

    @Operation(
            summary = "전체 세션 현황 조회",
            description = "모든 학습 세션을 최근 생성순으로 반환합니다. (페이지 지원)"
    )
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<AdminResponseDto.AdminSessionSummary>>> getSessions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<AdminResponseDto.AdminSessionSummary> sessions = adminService.getSessions(pageable);
        return ResponseEntity.ok(ApiResponse.success("전체 세션 현황을 조회했습니다.", sessions));
    }

    // ═══════════════════════════════════════════
    // 이야기 관리
    // ═══════════════════════════════════════════

    @Operation(summary = "이야기 목록 조회", description = "전체 이야기를 최근 생성순으로 반환합니다.")
    @GetMapping("/stories")
    public ResponseEntity<ApiResponse<Page<AdminResponseDto.StorySummary>>> getStories(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("이야기 목록을 조회했습니다.",
                adminService.getStories(pageable)));
    }

    @Operation(summary = "이야기 상세 조회 (장면 포함)", description = "이야기 정보와 장면 목록 전체를 반환합니다.")
    @GetMapping("/stories/{storyId}")
    public ResponseEntity<ApiResponse<AdminResponseDto.StoryDetail>> getStoryDetail(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId) {
        return ResponseEntity.ok(ApiResponse.success("이야기 상세를 조회했습니다.",
                adminService.getStoryDetail(storyId)));
    }

    @Operation(summary = "이야기 추가", description = "새 이야기 콘텐츠를 등록합니다. status를 draft로 설정하면 앱에 노출되지 않습니다.")
    @PostMapping("/stories")
    public ResponseEntity<ApiResponse<AdminResponseDto.StoryCreated>> addStory(
            @Valid @RequestBody AdminRequestDto.CreateStory request) {
        AdminResponseDto.StoryCreated story = adminService.addStory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이야기가 추가되었습니다.", story));
    }

    @Operation(summary = "이야기 수정")
    @PutMapping("/stories/{storyId}")
    public ResponseEntity<ApiResponse<AdminResponseDto.StoryCreated>> updateStory(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId,
            @Valid @RequestBody AdminRequestDto.UpdateStory request) {
        return ResponseEntity.ok(ApiResponse.success("이야기가 수정되었습니다.",
                adminService.updateStory(storyId, request)));
    }

    @Operation(summary = "이야기 장면 추가", description = "characterName이 null이면 내레이션 장면, non-null이면 대화 장면입니다.")
    @PostMapping("/stories/{storyId}/scenes")
    public ResponseEntity<ApiResponse<AdminResponseDto.SceneCreated>> addScene(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId,
            @Valid @RequestBody AdminRequestDto.CreateScene request) {
        AdminResponseDto.SceneCreated scene = adminService.addScene(storyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("장면이 추가되었습니다.", scene));
    }

    @Operation(summary = "장면 수정")
    @PutMapping("/stories/{storyId}/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<AdminResponseDto.SceneCreated>> updateScene(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId,
            @Parameter(description = "장면 ID") @PathVariable Long sceneId,
            @Valid @RequestBody AdminRequestDto.UpdateScene request) {
        return ResponseEntity.ok(ApiResponse.success("장면이 수정되었습니다.",
                adminService.updateScene(storyId, sceneId, request)));
    }

    @Operation(summary = "장면 삭제")
    @DeleteMapping("/stories/{storyId}/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<Void>> deleteScene(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId,
            @Parameter(description = "장면 ID") @PathVariable Long sceneId) {
        adminService.deleteScene(storyId, sceneId);
        return ResponseEntity.ok(ApiResponse.success("장면이 삭제되었습니다.", null));
    }
}
