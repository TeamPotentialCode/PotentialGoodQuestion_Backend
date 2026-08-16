package com.potential.goodquestion.domain.parent.service;

import com.potential.goodquestion.common.code.AuthErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.activity.repository.ActivityRepository;
import com.potential.goodquestion.domain.auth.repository.AuthRepository;
import com.potential.goodquestion.domain.child.repository.ChildConsentRepository;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.message.repository.MessageRepository;
import com.potential.goodquestion.domain.parent.dto.ParentRequestDto;
import com.potential.goodquestion.domain.parent.dto.ParentResponseDto;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import com.potential.goodquestion.domain.utterance.repository.UtteranceAnalysisRepository;
import com.potential.goodquestion.domain.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보호자 서비스
 *
 * 담당 API:
 * - GET    /api/parent/me : 내 정보 조회
 * - PATCH  /api/parent/me : 내 정보 수정
 * - DELETE /api/parent/me : 회원 탈퇴 (계정 및 연관 데이터 삭제)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;
    private final AuthRepository authRepository;
    private final ChildRepository childRepository;
    private final ChildConsentRepository childConsentRepository;
    private final StorySessionRepository storySessionRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository utteranceAnalysisRepository;
    private final ActivityRepository activityRepository;
    private final WordRepository wordRepository;

    public ParentResponseDto.Me getMe(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));
        return ParentResponseDto.Me.from(parent);
    }

    @Transactional
    public ParentResponseDto.Me updateMe(Long parentId, ParentRequestDto.Update request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));
        parent.updateName(request.getName());
        return ParentResponseDto.Me.from(parent);
    }

    /**
     * 회원 탈퇴 (계정 및 연관 데이터 삭제)
     *
     * 소프트 삭제(플래그 컬럼) 체계가 없는 프로젝트이므로 기존 삭제 구현
     * (WordService.deleteWord, AdminService.deleteScene)과 동일하게 하드 삭제한다.
     *
     * 보호자에 연결된 데이터를 외래키 역순으로 함께 삭제한다.
     *   utterance_analyses -> messages -> post_activity_results -> story_sessions
     *   -> word_books -> child_consents -> children -> auth_tokens -> parents
     * 엔티티에 cascade 설정이 없어 이 순서를 지키지 않으면 외래키 제약 위반이 발생한다.
     *
     * 탈퇴 후에는 저장된 리프레시 토큰이 삭제되므로 토큰 재발급이 불가능하다.
     * 다만 이미 발급된 액세스 토큰은 만료 전까지 유효하다.
     *
     * @param parentId JWT에서 추출한 보호자 ID
     */
    @Transactional
    public void withdraw(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));

        utteranceAnalysisRepository.deleteByParentId(parentId);
        messageRepository.deleteByParentId(parentId);
        activityRepository.deleteByParentId(parentId);
        storySessionRepository.deleteByParentId(parentId);
        wordRepository.deleteByParentId(parentId);
        childConsentRepository.deleteByParentId(parentId);
        childRepository.deleteByParentId(parentId);
        authRepository.deleteByParentId(parentId);

        parentRepository.delete(parent);
    }
}
