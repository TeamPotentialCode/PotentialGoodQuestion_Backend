package com.potential.goodquestion.domain.story.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.scene.dto.SceneResponseDto;
import com.potential.goodquestion.domain.scene.service.StorySceneService;
import com.potential.goodquestion.domain.story.dto.StoryResponseDto;
import com.potential.goodquestion.domain.story.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story", description = "이야기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;
    private final StorySceneService storySceneService;

    @Operation(summary = "이야기 목록 조회", description = "공개된 이야기 목록을 반환합니다. topic 파라미터로 주제 필터링이 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryResponseDto.StorySummary>>> getStories(
            @Parameter(description = "주제 필터 (예: 다름, 자기이해)") @RequestParam(required = false) String topic) {
        List<StoryResponseDto.StorySummary> stories = storyService.getStories(topic);
        return ResponseEntity.ok(ApiResponse.success("이야기 목록을 조회했습니다.", stories));
    }

    @Operation(summary = "이야기 상세 조회", description = "이야기 도입·상황·아이 역할 정보를 반환합니다.")
    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponseDto.StoryDetail>> getStoryDetail(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId) {
        StoryResponseDto.StoryDetail story = storyService.getStoryDetail(storyId);
        return ResponseEntity.ok(ApiResponse.success("이야기 상세를 조회했습니다.", story));
    }

    @Operation(summary = "장면 목록 조회", description = "이야기에 속한 모든 장면을 sceneOrder 오름차순으로 반환합니다. 이야기 시작 전 한 번만 호출해 클라이언트에 캐시하세요.")
    @GetMapping("/{storyId}/scenes")
    public ResponseEntity<ApiResponse<List<SceneResponseDto.SceneInfo>>> getScenes(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId) {
        List<SceneResponseDto.SceneInfo> scenes = storySceneService.getScenes(storyId);
        return ResponseEntity.ok(ApiResponse.success("장면 목록을 조회했습니다.", scenes));
    }

    @Operation(summary = "장면 정보 조회", description = "장면의 고정 첫 대사(character_opening) 및 상황 정보를 반환합니다. 대화 장면 진입 시 호출하세요.")
    @GetMapping("/{storyId}/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<SceneResponseDto.SceneInfo>> getScene(
            @Parameter(description = "이야기 ID") @PathVariable Long storyId,
            @Parameter(description = "장면 ID") @PathVariable Long sceneId) {
        SceneResponseDto.SceneInfo scene = storySceneService.getScene(storyId, sceneId);
        return ResponseEntity.ok(ApiResponse.success("장면 정보를 조회했습니다.", scene));
    }
}
