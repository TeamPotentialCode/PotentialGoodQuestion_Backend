package com.potential.goodquestion.domain.admin.service;

import com.potential.goodquestion.common.code.AdminErrorCode;
import com.potential.goodquestion.common.code.AuthErrorCode;
import com.potential.goodquestion.common.code.StoryErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.jwt.JwtUtil;
import com.potential.goodquestion.common.util.JsonUtils;
import com.potential.goodquestion.domain.admin.dto.AdminRequestDto;
import com.potential.goodquestion.domain.admin.dto.AdminResponseDto;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
import com.potential.goodquestion.domain.scene.repository.StorySceneRepository;
import com.potential.goodquestion.domain.story.entity.Story;
import com.potential.goodquestion.domain.story.repository.StoryRepository;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 서비스
 *
 * 담당 API:
 * - POST /api/admin/auth/login                       : 관리자 로그인 (코드 검증 후 어드민 JWT 발급)
 * - GET  /api/admin/dashboard                        : 전체 현황 대시보드
 * - GET  /api/admin/parents                          : 회원 목록 (페이지)
 * - GET  /api/admin/parents/{parentId}               : 회원 상세
 * - GET  /api/admin/parents/{parentId}/children      : 회원의 아이 목록
 * - GET  /api/admin/children/{childId}/sessions      : 아이 이야기 진척도
 * - GET  /api/admin/sessions                         : 전체 세션 현황 (페이지)
 * - POST   /api/admin/stories                              : 이야기 추가
 * - GET    /api/admin/stories                              : 이야기 목록
 * - GET    /api/admin/stories/{storyId}                   : 이야기 상세 (장면 포함)
 * - PUT    /api/admin/stories/{storyId}                   : 이야기 수정
 * - POST   /api/admin/stories/{storyId}/scenes            : 이야기 장면 추가
 * - PUT    /api/admin/stories/{storyId}/scenes/{sceneId}  : 장면 수정
 * - DELETE /api/admin/stories/{storyId}/scenes/{sceneId}  : 장면 삭제
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final StorySessionRepository storySessionRepository;
    private final StoryRepository storyRepository;
    private final StorySceneRepository storySceneRepository;
    private final JwtUtil jwtUtil;
    private final JsonUtils jsonUtils;

    @Value("${admin.code}")
    private String adminCode;

    // ═══════════════════════════════════════════
    // 관리자 로그인
    // ═══════════════════════════════════════════

    /**
     * 관리자 코드 검증 후 어드민 JWT 발급
     *
     * @param request adminCode 포함 요청
     * @return 어드민 Access Token
     */
    public AdminResponseDto.TokenInfo login(AdminRequestDto.Login request) {
        if (!adminCode.equals(request.getAdminCode())) {
            throw new CustomException(AdminErrorCode.INVALID_ADMIN_CODE);
        }
        return new AdminResponseDto.TokenInfo(jwtUtil.generateAdminToken());
    }

    // ═══════════════════════════════════════════
    // 대시보드
    // ═══════════════════════════════════════════

    /**
     * 전체 현황 대시보드 집계
     */
    public AdminResponseDto.DashboardInfo getDashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return new AdminResponseDto.DashboardInfo(
                parentRepository.count(),
                parentRepository.countByCreatedAtBetween(startOfDay, endOfDay),
                storySessionRepository.count(),
                storySessionRepository.countByStartedAtBetween(startOfDay, endOfDay),
                storySessionRepository.countByStatus("COMPLETED"),
                storySessionRepository.countByStatus("IN_PROGRESS"),
                storyRepository.countByStatus("published"),
                storyRepository.countByStatus("draft")
        );
    }

    // ═══════════════════════════════════════════
    // 회원 관리
    // ═══════════════════════════════════════════

    /**
     * 회원 목록 조회 (최근 가입순)
     *
     * @param pageable 페이지 정보
     * @return 보호자 요약 정보 페이지
     */
    public Page<AdminResponseDto.ParentSummary> getParents(Pageable pageable) {
        return parentRepository.findAll(pageable)
                .map(parent -> new AdminResponseDto.ParentSummary(
                        parent.getId(),
                        parent.getEmail(),
                        parent.getName(),
                        parent.getProvider().name(),
                        childRepository.countByParentId(parent.getId()),
                        parent.getCreatedAt()
                ));
    }

    /**
     * 회원 상세 조회 (아이 목록 포함)
     *
     * @param parentId 보호자 ID
     * @return 보호자 상세 정보
     */
    public AdminResponseDto.ParentDetail getParentDetail(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));

        List<AdminResponseDto.ChildSummary> children = childRepository.findByParentId(parentId)
                .stream()
                .map(child -> new AdminResponseDto.ChildSummary(
                        child.getId(),
                        child.getName(),
                        child.getBirthYear(),
                        child.calculateAge()
                ))
                .toList();

        return new AdminResponseDto.ParentDetail(
                parent.getId(),
                parent.getEmail(),
                parent.getName(),
                parent.getProvider().name(),
                parent.getCreatedAt(),
                children
        );
    }

    /**
     * 회원의 아이 목록 조회
     *
     * @param parentId 보호자 ID
     * @return 아이 목록
     */
    public List<AdminResponseDto.ChildSummary> getChildrenByParent(Long parentId) {
        if (!parentRepository.existsById(parentId)) {
            throw new CustomException(AuthErrorCode.PARENT_NOT_FOUND);
        }
        return childRepository.findByParentId(parentId)
                .stream()
                .map(child -> new AdminResponseDto.ChildSummary(
                        child.getId(),
                        child.getName(),
                        child.getBirthYear(),
                        child.calculateAge()
                ))
                .toList();
    }

    // ═══════════════════════════════════════════
    // 세션 관리
    // ═══════════════════════════════════════════

    /**
     * 아이 이야기 진척도 조회 (최근 활동순)
     *
     * @param childId 아이 ID
     * @return 아이의 세션 진척도 목록
     */
    public List<AdminResponseDto.SessionProgress> getChildSessions(Long childId) {
        return storySessionRepository.findByChildIdOrderByLastActivityAtDesc(childId)
                .stream()
                .map(session -> new AdminResponseDto.SessionProgress(
                        session.getId(),
                        session.getStory().getTitle(),
                        session.getStatus(),
                        session.getCurrentScene() != null
                                ? session.getCurrentScene().getSceneOrder() : null,
                        session.getCurrentChildTurnCount(),
                        session.getStartedAt(),
                        session.getCompletedAt(),
                        session.getLastActivityAt()
                ))
                .toList();
    }

    /**
     * 전체 세션 현황 조회 (페이지)
     *
     * @param pageable 페이지 정보
     * @return 세션 요약 페이지
     */
    @Transactional
    public Page<AdminResponseDto.AdminSessionSummary> getSessions(Pageable pageable) {
        return storySessionRepository.findAll(pageable)
                .map(session -> new AdminResponseDto.AdminSessionSummary(
                        session.getId(),
                        session.getChild().getName(),
                        session.getChild().getParent().getName(),
                        session.getStory().getTitle(),
                        session.getStatus(),
                        session.getStartedAt(),
                        session.getCompletedAt()
                ));
    }

    // ═══════════════════════════════════════════
    // 이야기 관리
    // ═══════════════════════════════════════════

    /**
     * 이야기 목록 조회 (최근 생성순)
     */
    public Page<AdminResponseDto.StorySummary> getStories(Pageable pageable) {
        return storyRepository.findAll(pageable)
                .map(story -> new AdminResponseDto.StorySummary(
                        story.getId(),
                        story.getTitle(),
                        story.getStatus(),
                        story.getDifficulty(),
                        storySceneRepository.findByStoryIdOrderBySceneOrder(story.getId()).size(),
                        story.getCreatedAt()
                ));
    }

    /**
     * 이야기 상세 조회 (장면 전체 포함)
     */
    public AdminResponseDto.StoryDetail getStoryDetail(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        List<AdminResponseDto.SceneDetail> scenes = storySceneRepository
                .findByStoryIdOrderBySceneOrder(storyId)
                .stream()
                .map(this::toSceneDetail)
                .toList();

        return new AdminResponseDto.StoryDetail(
                story.getId(),
                story.getTitle(),
                story.getSummary(),
                story.getDifficulty(),
                jsonUtils.toStringList(story.getTopics()),
                story.getThumbnailUrl(),
                story.getIntroduction(),
                story.getSituation(),
                story.getChildRole(),
                story.getEstimatedMinutes(),
                story.getPostActivityConfig(),
                story.getStatus(),
                story.getCreatedAt(),
                scenes
        );
    }

    /**
     * 이야기 수정
     */
    @Transactional
    public AdminResponseDto.StoryCreated updateStory(Long storyId, AdminRequestDto.UpdateStory request) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        story.update(
                request.getTitle(),
                request.getSummary(),
                request.getDifficulty(),
                jsonUtils.toJson(request.getTopics()),
                request.getThumbnailUrl(),
                request.getIntroduction(),
                request.getSituation(),
                request.getChildRole(),
                request.getEstimatedMinutes(),
                request.getPostActivityConfig(),
                request.getStatus()
        );
        return new AdminResponseDto.StoryCreated(story.getId(), story.getTitle(), story.getStatus());
    }

    /**
     * 장면 수정
     */
    @Transactional
    public AdminResponseDto.SceneCreated updateScene(Long storyId, Long sceneId,
                                                     AdminRequestDto.UpdateScene request) {
        if (!storyRepository.existsById(storyId)) {
            throw new CustomException(StoryErrorCode.STORY_NOT_FOUND);
        }
        StoryScene scene = storySceneRepository.findById(sceneId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.SCENE_NOT_FOUND));

        scene.update(
                request.getSceneOrder(),
                request.getSceneDescription(),
                request.getImageUrl(),
                request.getConflict(),
                request.getCharacterName(),
                request.getCharacterOpening(),
                request.getCharacterClosing(),
                request.getSceneGoal(),
                jsonUtils.toJson(request.getRequiredElements()),
                jsonUtils.toJson(request.getElementCriteria()),
                jsonUtils.toJson(request.getRemainingWorries()),
                request.isHasMission(),
                request.getPreferredTurns(),
                request.getMaxTurns()
        );
        return new AdminResponseDto.SceneCreated(scene.getId(), storyId, scene.getSceneOrder());
    }

    /**
     * 장면 삭제
     */
    @Transactional
    public void deleteScene(Long storyId, Long sceneId) {
        if (!storyRepository.existsById(storyId)) {
            throw new CustomException(StoryErrorCode.STORY_NOT_FOUND);
        }
        StoryScene scene = storySceneRepository.findById(sceneId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.SCENE_NOT_FOUND));
        storySceneRepository.delete(scene);
    }

    private AdminResponseDto.SceneDetail toSceneDetail(StoryScene scene) {
        return new AdminResponseDto.SceneDetail(
                scene.getId(),
                scene.getSceneOrder(),
                scene.getSceneDescription(),
                scene.getImageUrl(),
                scene.getConflict(),
                scene.getCharacterName(),
                scene.getCharacterOpening(),
                scene.getCharacterClosing(),
                scene.getSceneGoal(),
                jsonUtils.toStringList(scene.getRequiredElements()),
                jsonUtils.toStringMap(scene.getElementCriteria()),
                jsonUtils.toStringMap(scene.getRemainingWorries()),
                scene.isHasMission(),
                scene.getPreferredTurns(),
                scene.getMaxTurns()
        );
    }

    /**
     * 이야기 추가
     *
     * @param request 이야기 정보
     * @return 생성된 이야기 ID·제목·상태
     */
    @Transactional
    public AdminResponseDto.StoryCreated addStory(AdminRequestDto.CreateStory request) {
        Story story = Story.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .difficulty(request.getDifficulty())
                .topics(jsonUtils.toJson(request.getTopics()))
                .thumbnailUrl(request.getThumbnailUrl())
                .introduction(request.getIntroduction())
                .situation(request.getSituation())
                .childRole(request.getChildRole())
                .estimatedMinutes(request.getEstimatedMinutes())
                .postActivityConfig(request.getPostActivityConfig())
                .status(request.getStatus() != null ? request.getStatus() : "published")
                .build();

        Story saved = storyRepository.save(story);
        return new AdminResponseDto.StoryCreated(saved.getId(), saved.getTitle(), saved.getStatus());
    }

    /**
     * 이야기 장면 추가
     *
     * @param storyId 장면을 추가할 이야기 ID
     * @param request 장면 정보
     * @return 생성된 장면 ID·이야기 ID·장면 순서
     */
    @Transactional
    public AdminResponseDto.SceneCreated addScene(Long storyId, AdminRequestDto.CreateScene request) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        StoryScene scene = StoryScene.builder()
                .story(story)
                .sceneOrder(request.getSceneOrder())
                .sceneDescription(request.getSceneDescription())
                .imageUrl(request.getImageUrl())
                .conflict(request.getConflict())
                .characterName(request.getCharacterName())
                .characterOpening(request.getCharacterOpening())
                .characterClosing(request.getCharacterClosing())
                .sceneGoal(request.getSceneGoal())
                .requiredElements(jsonUtils.toJson(request.getRequiredElements()))
                .elementCriteria(jsonUtils.toJson(request.getElementCriteria()))
                .remainingWorries(jsonUtils.toJson(request.getRemainingWorries()))
                .hasMission(request.isHasMission())
                .preferredTurns(request.getPreferredTurns())
                .maxTurns(request.getMaxTurns())
                .build();

        StoryScene saved = storySceneRepository.save(scene);
        return new AdminResponseDto.SceneCreated(saved.getId(), storyId, saved.getSceneOrder());
    }
}
