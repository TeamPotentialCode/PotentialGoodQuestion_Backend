package com.potential.goodquestion.domain.storysession.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 아이의 이야기 학습 세션 엔티티
 *
 * 테이블명: story_sessions
 *
 * 담당 필드 설명:
 * - currentSceneId: 현재 진행 중인 장면 ID
 *   ⚠️ 전우선 담당: StoryScene 엔티티 생성 후 @ManyToOne StoryScene currentScene 으로 교체
 * - accumulatedElements: 누적된 사고 요소 코드 배열 (JSON 문자열 ex. ["REASON","PERSPECTIVE"])
 * - lastDetectedElements: 직전 턴에서 탐지된 요소 (JSON 문자열)
 * - lastResponseMode: 직전 응답 모드 (NORMAL / GUIDED / CLOSING)
 * - lastGuidanceTarget: 직전에 유도한 사고 요소 코드
 * - turnsWithoutNewElement: 새 요소 없이 지나간 연속 턴 수
 * - consecutiveLowInformationTurns: 저정보 발화(SHORT/UNCLEAR/OFF_TOPIC) 연속 횟수
 * - sceneGoalMet: 현재 장면 목표 달성 여부
 * - sceneEndReason: 장면 종료 이유 (GOAL_MET / MAX_TURNS)
 * - status: 세션 상태 문자열 (IN_PROGRESS / COMPLETED)
 */
@Comment("아이의 이야기 학습 세션")
@Entity
@Table(name = "story_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorySession extends BaseEntity {

    @Comment("세션 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("학습 아이")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Comment("이야기")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Comment("현재 진행 중인 장면")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_scene_id")
    private StoryScene currentScene;

    @Comment("현재 장면 아이 발화 횟수")
    @Column(name = "current_child_turn_count", nullable = false)
    private int currentChildTurnCount = 0;

    @Comment("누적 사고 요소 JSON 배열 ex. [\"REASON\",\"PERSPECTIVE\"]")
    @Column(name = "accumulated_elements", columnDefinition = "TEXT")
    private String accumulatedElements = "[]";

    @Comment("직전 턴 탐지 요소 JSON 배열")
    @Column(name = "last_detected_elements", columnDefinition = "TEXT")
    private String lastDetectedElements = "[]";

    @Comment("직전 응답 모드 (NORMAL / GUIDED / CLOSING)")
    @Column(name = "last_response_mode", length = 20)
    private String lastResponseMode;

    @Comment("직전 유도 대상 사고 요소 코드")
    @Column(name = "last_guidance_target", length = 30)
    private String lastGuidanceTarget;

    @Comment("신규 요소 없는 연속 턴 수")
    @Column(name = "turns_without_new_element", nullable = false)
    private int turnsWithoutNewElement = 0;

    @Comment("저정보 발화 연속 횟수 (SHORT/UNCLEAR/OFF_TOPIC 연속)")
    @Column(name = "consecutive_low_information_turns", nullable = false)
    private int consecutiveLowInformationTurns = 0;

    @Comment("장면 목표 달성 여부")
    @Column(name = "scene_goal_met", nullable = false)
    private boolean sceneGoalMet = false;

    @Comment("장면 종료 이유 (GOAL_MET / MAX_TURNS)")
    @Column(name = "scene_end_reason", length = 20)
    private String sceneEndReason;

    @Comment("세션 상태 (IN_PROGRESS / COMPLETED)")
    @Column(name = "status", length = 20, nullable = false)
    private String status = "IN_PROGRESS";

    @Comment("세션 시작 일시")
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Comment("세션 완료 일시")
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Comment("마지막 활동 일시")
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Builder
    public StorySession(Child child, Story story, StoryScene currentScene) {
        this.child = child;
        this.story = story;
        this.currentScene = currentScene;
        this.startedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public static StorySession create(Child child, Story story, StoryScene firstScene) {
        return StorySession.builder()
                .child(child)
                .story(story)
                .currentScene(firstScene)
                .build();
    }

    /**
     * 발화 처리 후 세션 상태 일괄 갱신
     * 전우선 UtteranceService에서 호출
     *
     * @param newAccumulatedElements  갱신된 누적 요소 JSON
     * @param newDetectedElements     이번 턴 탐지 요소 JSON
     * @param responseMode            응답 모드 (NORMAL/GUIDED/CLOSING)
     * @param guidanceTarget          유도 대상 요소 (GUIDED일 때만)
     * @param turnsWithoutNewElement  신규 요소 없는 연속 턴 수
     * @param consecutiveLowInfoTurns 저정보 발화 연속 횟수
     * @param goalMet                 장면 목표 달성 여부
     * @param endReason               장면 종료 이유
     */
    public void updateAfterUtterance(
            String newAccumulatedElements,
            String newDetectedElements,
            String responseMode,
            String guidanceTarget,
            int turnsWithoutNewElement,
            int consecutiveLowInfoTurns,
            boolean goalMet,
            String endReason
    ) {
        this.currentChildTurnCount++;
        this.accumulatedElements = newAccumulatedElements;
        this.lastDetectedElements = newDetectedElements;
        this.lastResponseMode = responseMode;
        if (guidanceTarget != null) {
            this.lastGuidanceTarget = guidanceTarget;
        }
        this.turnsWithoutNewElement = turnsWithoutNewElement;
        this.consecutiveLowInformationTurns = consecutiveLowInfoTurns;
        this.sceneGoalMet = goalMet;
        this.sceneEndReason = endReason;
        this.lastActivityAt = LocalDateTime.now();
    }

    public void advanceScene(StoryScene nextScene) {
        this.currentScene = nextScene;
        this.currentChildTurnCount = 0;
        this.accumulatedElements = "[]";
        this.lastDetectedElements = "[]";
        this.lastResponseMode = null;
        this.lastGuidanceTarget = null;
        this.turnsWithoutNewElement = 0;
        this.consecutiveLowInformationTurns = 0;
        this.sceneGoalMet = false;
        this.sceneEndReason = null;
    }

    /**
     * 세션 완료 처리 (마지막 장면 종료 시)
     */
    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }
}
