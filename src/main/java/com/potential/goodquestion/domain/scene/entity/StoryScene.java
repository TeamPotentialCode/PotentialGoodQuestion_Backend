package com.potential.goodquestion.domain.scene.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.story.entity.Story;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("이야기 장면")
@Entity
@Table(name = "story_scenes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryScene extends BaseEntity {

    @Comment("장면 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("이야기")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Comment("장면 순서 (1부터 시작)")
    @Column(name = "scene_order", nullable = false)
    private Integer sceneOrder;

    @Comment("장면 상황 설명 (고정, 수정 불가)")
    @Column(name = "scene_description", columnDefinition = "TEXT", nullable = false)
    private String sceneDescription;

    @Comment("장면 배경 이미지 URL (장면 화면 표시용)")
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Comment("갈등 요약")
    @Column(name = "conflict", columnDefinition = "TEXT")
    private String conflict;

    @Comment("캐릭터 이름")
    @Column(name = "character_name", length = 50)
    private String characterName;

    @Comment("고정 첫 대사 (수정 불가)")
    @Column(name = "character_opening", columnDefinition = "TEXT")
    private String characterOpening;

    @Comment("고정 마지막 대사 (수정 불가)")
    @Column(name = "character_closing", columnDefinition = "TEXT")
    private String characterClosing;

    @Comment("장면 학습 목표")
    @Column(name = "scene_goal", columnDefinition = "TEXT")
    private String sceneGoal;

    @Comment("필수 사고 요소 JSON 배열 ex) [\"REASON\",\"PERSPECTIVE\",\"SOLUTION\"]")
    @Column(name = "required_elements", columnDefinition = "TEXT")
    private String requiredElements;

    @Comment("요소별 인정 기준 JSON 객체")
    @Column(name = "element_criteria", columnDefinition = "TEXT")
    private String elementCriteria;

    @Comment("캐릭터 걱정 정보 JSON 객체 (유도 시 사용)")
    @Column(name = "remaining_worries", columnDefinition = "TEXT")
    private String remainingWorries;

    @Comment("미션 포함 여부")
    @Column(name = "has_mission", nullable = false)
    private boolean hasMission = false;

    @Comment("권장 최소 턴 수")
    @Column(name = "preferred_turns", nullable = false)
    private Integer preferredTurns;

    @Comment("최대 허용 턴 수")
    @Column(name = "max_turns", nullable = false)
    private Integer maxTurns;

    public void update(Integer sceneOrder, String sceneDescription, String imageUrl,
                       String conflict, String characterName, String characterOpening,
                       String characterClosing, String sceneGoal, String requiredElements,
                       String elementCriteria, String remainingWorries,
                       boolean hasMission, Integer preferredTurns, Integer maxTurns) {
        this.sceneOrder = sceneOrder;
        this.sceneDescription = sceneDescription;
        this.imageUrl = imageUrl;
        this.conflict = conflict;
        this.characterName = characterName;
        this.characterOpening = characterOpening;
        this.characterClosing = characterClosing;
        this.sceneGoal = sceneGoal;
        this.requiredElements = requiredElements;
        this.elementCriteria = elementCriteria;
        this.remainingWorries = remainingWorries;
        this.hasMission = hasMission;
        this.preferredTurns = preferredTurns;
        this.maxTurns = maxTurns;
    }

    /**
     * 장면 생성 (관리자 전용)
     */
    @Builder
    public StoryScene(Story story, Integer sceneOrder, String sceneDescription, String imageUrl,
                      String conflict, String characterName, String characterOpening,
                      String characterClosing, String sceneGoal, String requiredElements,
                      String elementCriteria, String remainingWorries,
                      boolean hasMission, Integer preferredTurns, Integer maxTurns) {
        this.story = story;
        this.sceneOrder = sceneOrder;
        this.sceneDescription = sceneDescription;
        this.imageUrl = imageUrl;
        this.conflict = conflict;
        this.characterName = characterName;
        this.characterOpening = characterOpening;
        this.characterClosing = characterClosing;
        this.sceneGoal = sceneGoal;
        this.requiredElements = requiredElements;
        this.elementCriteria = elementCriteria;
        this.remainingWorries = remainingWorries;
        this.hasMission = hasMission;
        this.preferredTurns = preferredTurns;
        this.maxTurns = maxTurns;
    }
}
