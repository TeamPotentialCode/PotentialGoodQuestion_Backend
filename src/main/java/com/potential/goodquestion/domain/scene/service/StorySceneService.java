package com.potential.goodquestion.domain.scene.service;

import com.potential.goodquestion.common.code.StoryErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.scene.dto.SceneResponseDto;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
import com.potential.goodquestion.domain.scene.repository.StorySceneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * StoryScene 서비스
 *
 * 담당 API:
 * - GET /api/stories/{storyId}/scenes/{sceneId} : 장면 정보 조회 (고정 첫 대사 포함)
 *
 * sceneId 는 장면의 PK(StoryScene.id)다.
 * (DB_구조 기준: story_sessions.current_scene_id, messages.scene_id 모두 story_scenes.id 를 참조)
 * 요청한 storyId 에 속한 장면인지 함께 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorySceneService {

    private final StorySceneRepository storySceneRepository;

    /**
     * 장면 정보 조회
     *
     * @param storyId 이야기 ID (소속 검증용)
     * @param sceneId 장면 ID (StoryScene.id)
     * @return 장면 정보 (고정 첫 대사 포함)
     */
    public SceneResponseDto.SceneInfo getScene(Long storyId, Long sceneId) {
        StoryScene scene = storySceneRepository.findById(sceneId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.SCENE_NOT_FOUND));

        // 요청한 이야기에 속한 장면이 아니면 조회 불가
        if (!scene.getStory().getId().equals(storyId)) {
            throw new CustomException(StoryErrorCode.SCENE_NOT_FOUND);
        }

        return SceneResponseDto.SceneInfo.from(scene);
    }
}
