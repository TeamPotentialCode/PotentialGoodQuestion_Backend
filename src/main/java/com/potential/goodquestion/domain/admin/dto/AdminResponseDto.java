package com.potential.goodquestion.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 관리자 API 응답 DTO 모음
 */
public class AdminResponseDto {

    /**
     * 관리자 로그인 응답 (Access Token)
     */
    public record TokenInfo(
            String accessToken
    ) {}

    /**
     * 전체 현황 대시보드
     */
    public record DashboardInfo(
            /** 총 보호자 수 */
            long totalParents,
            /** 오늘 신규 가입 보호자 수 */
            long todayNewParents,
            /** 총 세션 수 */
            long totalSessions,
            /** 오늘 시작된 세션 수 */
            long todaySessions,
            /** 완료된 세션 수 */
            long completedSessions,
            /** 진행 중인 세션 수 */
            long inProgressSessions,
            /** 공개 이야기 수 */
            long publishedStories,
            /** 비공개(draft) 이야기 수 */
            long draftStories
    ) {}

    /**
     * 회원 목록 아이템 (페이지 조회용)
     */
    public record ParentSummary(
            Long id,
            String email,
            String name,
            String provider,
            long childCount,
            LocalDateTime createdAt
    ) {}

    /**
     * 회원 상세
     */
    public record ParentDetail(
            Long id,
            String email,
            String name,
            String provider,
            LocalDateTime createdAt,
            List<ChildSummary> children
    ) {}

    /**
     * 아이 요약 정보
     */
    public record ChildSummary(
            Long id,
            String name,
            Integer birthYear,
            int age
    ) {}

    /**
     * 아이의 이야기 진척도 (세션 단위)
     */
    public record SessionProgress(
            Long sessionId,
            String storyTitle,
            /** IN_PROGRESS / COMPLETED */
            String status,
            /** 현재 진행 중인 장면 순서 (완료 세션이면 null 가능) */
            Integer currentSceneOrder,
            /** 현재 장면 아이 발화 횟수 */
            int childTurnCount,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime lastActivityAt
    ) {}

    /**
     * 전체 세션 목록 아이템 (관리자 세션 현황용)
     */
    public record AdminSessionSummary(
            Long sessionId,
            String childName,
            String parentName,
            String storyTitle,
            String status,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {}

    /**
     * 이야기 추가 결과
     */
    public record StoryCreated(
            Long storyId,
            String title,
            String status
    ) {}

    /**
     * 장면 추가 결과
     */
    public record SceneCreated(
            Long sceneId,
            Long storyId,
            Integer sceneOrder
    ) {}

    /**
     * 이야기 목록 아이템
     */
    public record StorySummary(
            Long id,
            String title,
            String status,
            String difficulty,
            int sceneCount,
            LocalDateTime createdAt
    ) {}

    /**
     * 장면 상세 (수정용)
     */
    public record SceneDetail(
            Long id,
            Integer sceneOrder,
            String sceneDescription,
            String imageUrl,
            String conflict,
            String characterName,
            String characterOpening,
            String characterClosing,
            String sceneGoal,
            List<String> requiredElements,
            Map<String, String> elementCriteria,
            Map<String, String> remainingWorries,
            boolean hasMission,
            Integer preferredTurns,
            Integer maxTurns
    ) {}

    /**
     * 이야기 상세 + 장면 목록 (수정용)
     */
    public record StoryDetail(
            Long id,
            String title,
            String summary,
            String difficulty,
            List<String> topics,
            String thumbnailUrl,
            String introduction,
            String situation,
            String childRole,
            Integer estimatedMinutes,
            String postActivityConfig,
            String status,
            LocalDateTime createdAt,
            List<SceneDetail> scenes
    ) {}
}
