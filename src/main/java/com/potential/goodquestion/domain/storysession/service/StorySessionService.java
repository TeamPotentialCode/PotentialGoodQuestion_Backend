package com.potential.goodquestion.domain.storysession.service;

import com.potential.goodquestion.common.code.ChildErrorCode;
import com.potential.goodquestion.common.code.SessionErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.repository.ChildConsentRepository;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.message.repository.MessageRepository;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
import com.potential.goodquestion.domain.scene.repository.StorySceneRepository;
import com.potential.goodquestion.domain.storysession.dto.StorySessionRequestDto;
import com.potential.goodquestion.domain.storysession.dto.StorySessionResponseDto;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import com.potential.goodquestion.domain.story.entity.Story;
import com.potential.goodquestion.domain.story.repository.StoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스토리 세션 서비스
 *
 * 담당 API:
 * - POST /api/stories/{storyId}/sessions : 새 학습 세션 시작
 * - GET  /api/sessions/{sessionId}       : 세션 정보 조회 (이어하기 복귀)
 *
 * 보안:
 * - createSession: 보호자 소유 아이인지 검증 + 유효한 동의 존재 여부 검증
 * - getSession: 해당 아이의 보호자인지 검증
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorySessionService {

    private final StorySessionRepository storySessionRepository;
    private final StoryRepository storyRepository;
    private final ChildRepository childRepository;
    private final ChildConsentRepository childConsentRepository;
    private final StorySceneRepository storySceneRepository;
    private final MessageRepository messageRepository;

    /**
     * 새 학습 세션 생성
     *
     * 동일 아이 + 동일 이야기의 IN_PROGRESS 세션이 이미 있으면 해당 세션을 반환
     * 없을 경우에만 새 세션을 생성
     *
     * @param storyId  이야기 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     * @param request  childId 포함
     * @return 기존 또는 새로 생성된 세션 정보
     */
    @Transactional
    public StorySessionResponseDto.SessionInfo createSession(Long storyId, Long parentId,
            StorySessionRequestDto.Create request) {

        // 이야기 조회
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.STORY_NOT_FOUND));

        // 아이 조회 + 보호자 소유 검증
        Child child = getChildWithOwnerCheck(parentId, request.getChildId());

        // 동의 여부 검증: 유효한 동의(철회되지 않은)가 없으면 세션 시작 불가
        boolean hasActiveConsent = childConsentRepository.existsActiveConsentByChild(child);
        if (!hasActiveConsent) {
            throw new CustomException(ChildErrorCode.CONSENT_NOT_FOUND);
        }

        // 이미 진행 중인 세션이 있으면 기존 세션 반환
        Optional<StorySession> existing = storySessionRepository
                .findByChildIdAndStoryIdAndStatus(child.getId(), story.getId(), "IN_PROGRESS");
        if (existing.isPresent()) {
            return StorySessionResponseDto.SessionInfo.from(existing.get());
        }

        // 세션 생성 및 저장
        StoryScene firstScene = storySceneRepository.findByStoryIdAndSceneOrder(story.getId(), 1)
                .orElse(null);
        StorySession session = StorySession.create(child, story, firstScene);
        return StorySessionResponseDto.SessionInfo.from(storySessionRepository.save(session));
    }

    /**
     * 세션 정보 조회 (이어하기 복귀)
     *
     * @param sessionId 세션 ID
     * @param parentId  JWT에서 추출한 보호자 ID (소유권 검증)
     * @return 세션 정보 (currentSceneId + 대화 메시지 이력 포함)
     */
    public StorySessionResponseDto.SessionInfo getSession(Long sessionId, Long parentId) {

        StorySession session = storySessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        // 해당 세션의 아이가 이 보호자 소유인지 검증
        if (!session.getChild().getParent().getId().equals(parentId)) {
            throw new CustomException(SessionErrorCode.SESSION_ACCESS_DENIED);
        }

        // 세션 대화 이력 조회 (발화 순서 오름차순)
        List<StorySessionResponseDto.MessageInfo> messages = messageRepository
                .findBySessionIdOrderByTurnOrderAsc(sessionId)
                .stream()
                .map(StorySessionResponseDto.MessageInfo::from)
                .toList();

        return StorySessionResponseDto.SessionInfo.from(session, messages);
    }

    // ─────────── private ────────────────

    /**
     * 아이 조회 + 보호자 소유권 검증
     * 아이가 없거나 다른 보호자 소유이면 예외 발생
     */
    private Child getChildWithOwnerCheck(Long parentId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CHILD_NOT_FOUND));

        if (!child.getParent().getId().equals(parentId)) {
            throw new CustomException(SessionErrorCode.SESSION_ACCESS_DENIED);
        }
        return child;
    }
}
