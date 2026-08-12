package com.potential.goodquestion.domain.story.service;

import com.potential.goodquestion.common.code.StoryErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.util.JsonUtils;
import com.potential.goodquestion.domain.story.dto.StoryResponseDto;
import com.potential.goodquestion.domain.story.entity.Story;
import com.potential.goodquestion.domain.story.repository.StoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Story 서비스
 *
 * 담당 API:
 * - GET /api/stories              : 이야기 목록 조회 (주제 필터 포함)
 * - GET /api/stories/{storyId}    : 이야기 상세 조회 (도입·상황·아이 역할)
 *
 * 공개 상태(published) 이야기만 노출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    /** 노출 대상 공개 상태 값 */
    private static final String STATUS_PUBLISHED = "published";

    private final StoryRepository storyRepository;
    private final JsonUtils jsonUtils;

    /**
     * 이야기 목록 조회
     *
     * @param topic 주제 필터 (null 또는 공백이면 전체 조회)
     * @return 이야기 목록
     */
    public List<StoryResponseDto.StorySummary> getStories(String topic) {
        List<Story> stories = StringUtils.hasText(topic)
                ? storyRepository.findByStatusAndTopicContaining(STATUS_PUBLISHED, topic.trim())
                : storyRepository.findByStatusOrderByIdAsc(STATUS_PUBLISHED);

        return stories.stream()
                .map(story -> StoryResponseDto.StorySummary.of(story, jsonUtils.toStringList(story.getTopics())))
                .toList();
    }

    /**
     * 이야기 상세 조회
     *
     * @param storyId 이야기 ID
     * @return 이야기 상세 (도입·상황·아이 역할 포함)
     */
    public StoryResponseDto.StoryDetail getStoryDetail(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new CustomException(StoryErrorCode.STORY_NOT_FOUND));

        return StoryResponseDto.StoryDetail.of(story, jsonUtils.toStringList(story.getTopics()));
    }
}
