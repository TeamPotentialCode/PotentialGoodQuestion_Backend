package com.potential.goodquestion.domain.home.service;

import com.potential.goodquestion.common.code.ChildErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.util.JsonUtils;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.home.dto.HomeResponseDto;
import com.potential.goodquestion.domain.story.dto.StoryResponseDto;
import com.potential.goodquestion.domain.story.repository.StoryRepository;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 서비스
 *
 * 담당 API:
 * - GET /api/home?childId= : 이어하기 + 추천 이야기
 *
 * 보안:
 * - childId 가 로그인 보호자 소유 아이인지 검증
 *
 * 추천 로직은 MVP 미구현으로, 공개(published) 이야기 상위 N개를 노출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PUBLISHED = "published";
    private static final int RECOMMEND_LIMIT = 3;

    private final ChildRepository childRepository;
    private final StorySessionRepository storySessionRepository;
    private final StoryRepository storyRepository;
    private final JsonUtils jsonUtils;

    /**
     * 홈 화면 조회
     *
     * @param childId  대상 아이 ID
     * @param parentId JWT 보호자 ID (소유권 검증)
     * @return 이어하기 세션 + 추천 이야기 목록
     */
    public HomeResponseDto.HomeInfo getHome(Long childId, Long parentId) {
        verifyChildOwner(childId, parentId);

        HomeResponseDto.ContinueInfo continueSession = storySessionRepository
                .findFirstByChildIdAndStatusOrderByLastActivityAtDesc(childId, STATUS_IN_PROGRESS)
                .map(HomeResponseDto.ContinueInfo::from)
                .orElse(null);

        List<StoryResponseDto.StorySummary> recommended = storyRepository
                .findByStatusOrderByIdAsc(STATUS_PUBLISHED).stream()
                .limit(RECOMMEND_LIMIT)
                .map(story -> StoryResponseDto.StorySummary.of(story, jsonUtils.toStringList(story.getTopics())))
                .toList();

        return HomeResponseDto.HomeInfo.builder()
                .continueSession(continueSession)
                .recommendedStories(recommended)
                .build();
    }

    /**
     * 아이 소유권 검증 (없거나 다른 보호자 소유이면 예외)
     */
    private void verifyChildOwner(Long childId, Long parentId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CHILD_NOT_FOUND));
        if (!child.getParent().getId().equals(parentId)) {
            throw new CustomException(ChildErrorCode.CHILD_ACCESS_DENIED);
        }
    }
}
