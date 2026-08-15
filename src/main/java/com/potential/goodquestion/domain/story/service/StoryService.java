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

    /** 주제 조회 LIKE 패턴의 이스케이프 문자 (StoryRepository 쿼리의 ESCAPE 와 동일해야 함) */
    private static final char LIKE_ESCAPE = '!';

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
                ? storyRepository.findByStatusAndTopicElement(STATUS_PUBLISHED, escapeLike(topic.trim()))
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

    // ─────────── private ────────────────

    /**
     * LIKE 패턴 특수문자를 이스케이프한다.
     *
     * 이스케이프하지 않으면 topic 에 들어온 % 나 _ 가 와일드카드로 해석되어
     * ?topic=% 같은 값 하나로 필터가 무력화된다.
     */
    private String escapeLike(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
