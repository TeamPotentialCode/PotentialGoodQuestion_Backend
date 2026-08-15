package com.potential.goodquestion.domain.story.service;

import com.potential.goodquestion.common.code.StoryErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.util.JsonUtils;
import com.potential.goodquestion.domain.story.dto.StoryResponseDto;
import com.potential.goodquestion.domain.story.entity.Story;
import com.potential.goodquestion.domain.story.repository.StoryRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Story 서비스
 *
 * 담당 API:
 * - GET /api/stories              : 이야기 목록 조회 (주제 필터 포함, 다중 주제 OR)
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
     * 화면에서 주제를 여러 개 선택할 수 있으므로 다중 주제를 OR 로 매칭한다.
     * (선택한 주제 중 하나라도 가진 이야기를 노출)
     *
     * 필터는 topics JSON 을 파싱한 원소와의 정확 일치로 판정한다.
     * "자기" 처럼 원소의 일부만 보내는 경우는 매칭하지 않는다.
     *
     * @param topics 주제 필터 목록 (null 또는 비어있으면 전체 조회)
     *               ?topic=다름&topic=자기이해 또는 ?topic=다름,자기이해 형태 모두 지원
     * @return 이야기 목록 (등록 순)
     */
    public List<StoryResponseDto.StorySummary> getStories(List<String> topics) {
        Set<String> filter = normalize(topics);

        return storyRepository.findByStatusOrderByIdAsc(STATUS_PUBLISHED).stream()
                .map(story -> new StoryWithTopics(story, jsonUtils.toStringList(story.getTopics())))
                .filter(st -> filter.isEmpty() || st.topics().stream().anyMatch(filter::contains))
                .map(st -> StoryResponseDto.StorySummary.of(st.story(), st.topics()))
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
     * 주제 필터 정리: null 제거, 공백 제거(trim), 빈 값 제외
     */
    private Set<String> normalize(List<String> topics) {
        if (topics == null) {
            return Set.of();
        }
        return topics.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * 이야기와 파싱된 주제 목록 (필터 판정과 응답 변환에서 파싱을 한 번만 하기 위함)
     */
    private record StoryWithTopics(Story story, List<String> topics) {
    }
}
