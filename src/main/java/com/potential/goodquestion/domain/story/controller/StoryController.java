package com.potential.goodquestion.domain.story.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.domain.scene.dto.SceneResponseDto;
import com.potential.goodquestion.domain.scene.service.StorySceneService;
import com.potential.goodquestion.domain.story.dto.StoryResponseDto;
import com.potential.goodquestion.domain.story.service.StoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Story 컨트롤러
 *
 * GET /api/stories                          : 이야기 목록 조회 (주제 필터 포함)
 * GET /api/stories/{storyId}                : 이야기 상세 조회 (도입·상황·아이 역할)
 * GET /api/stories/{storyId}/scenes/{sceneId} : 장면 정보 조회 (고정 첫 대사 포함)
 *
 * 모든 엔드포인트는 JWT 인증 필수 (SecurityConfig anyRequest().authenticated())
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;
    private final StorySceneService storySceneService;

    /**
     * 이야기 목록 조회
     * GET /api/stories
     * GET /api/stories?topic=다름
     *
     * @param topic 주제 필터 (선택). 미지정 시 전체 공개 이야기 조회
     * @return 이야기 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryResponseDto.StorySummary>>> getStories(
            @RequestParam(required = false) String topic) {

        List<StoryResponseDto.StorySummary> stories = storyService.getStories(topic);
        return ResponseEntity.ok(ApiResponse.success("이야기 목록을 조회했습니다.", stories));
    }

    /**
     * 이야기 상세 조회
     * GET /api/stories/{storyId}
     *
     * @param storyId 이야기 ID
     * @return 이야기 상세 (도입·상황·아이 역할)
     */
    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryResponseDto.StoryDetail>> getStoryDetail(
            @PathVariable Long storyId) {

        StoryResponseDto.StoryDetail story = storyService.getStoryDetail(storyId);
        return ResponseEntity.ok(ApiResponse.success("이야기 상세를 조회했습니다.", story));
    }

    /**
     * 장면 정보 조회
     * GET /api/stories/{storyId}/scenes/{sceneId}
     *
     * @param storyId 이야기 ID (장면 소속 검증용)
     * @param sceneId 장면 ID
     * @return 장면 정보 (고정 첫 대사 포함)
     */
    @GetMapping("/{storyId}/scenes/{sceneId}")
    public ResponseEntity<ApiResponse<SceneResponseDto.SceneInfo>> getScene(
            @PathVariable Long storyId,
            @PathVariable Long sceneId) {

        SceneResponseDto.SceneInfo scene = storySceneService.getScene(storyId, sceneId);
        return ResponseEntity.ok(ApiResponse.success("장면 정보를 조회했습니다.", scene));
    }
}
