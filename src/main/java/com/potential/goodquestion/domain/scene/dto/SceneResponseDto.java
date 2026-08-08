package com.potential.goodquestion.domain.scene.dto;

import com.potential.goodquestion.domain.scene.entity.StoryScene;
import lombok.Builder;
import lombok.Getter;

/**
 * Scene 응답 DTO 모음
 */
public class SceneResponseDto {

    /**
     * 장면 정보 (클라이언트 장면 화면 표시용)
     * GET /api/stories/{storyId}/scenes/{sceneId} 에서 사용
     *
     * 노출 필드는 MVP 장면 화면 요소로 한정한다: 상황 설명, 캐릭터, 고정 첫 대사.
     *
     * 아래 필드는 서버 측 대화/분석 로직에서만 사용하므로 클라이언트에 노출하지 않는다.
     * (DB_구조 · 발화 분석 기준 문서에서 서버 판단용으로 명시)
     *  - sceneGoal / requiredElements / elementCriteria / remainingWorries : 발화 분석·진행 판단 기준
     *  - preferredTurns / maxTurns                                         : 종료 판단 임계값
     *  - characterClosing                                                  : 종료(CLOSING) 시 서버가 조회해 재생
     */
    @Getter
    @Builder
    public static class SceneInfo {

        private Long sceneId;
        private Long storyId;
        private Integer sceneOrder;
        private String imageUrl;          // 장면 배경 이미지 URL
        private String sceneDescription;  // 장면 상황 설명 (고정)
        private String characterName;     // 대화 캐릭터 이름 (내레이션 전용 장면이면 null)
        private String characterOpening;  // 고정 첫 대사 (수정 불가)

        /**
         * StoryScene 엔티티 → SceneInfo 변환
         */
        public static SceneInfo from(StoryScene scene) {
            return SceneInfo.builder()
                    .sceneId(scene.getId())
                    .storyId(scene.getStory().getId())
                    .sceneOrder(scene.getSceneOrder())
                    .imageUrl(scene.getImageUrl())
                    .sceneDescription(scene.getSceneDescription())
                    .characterName(scene.getCharacterName())
                    .characterOpening(scene.getCharacterOpening())
                    .build();
        }
    }
}
