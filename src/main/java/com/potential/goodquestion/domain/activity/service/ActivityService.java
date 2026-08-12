package com.potential.goodquestion.domain.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potential.goodquestion.common.code.ActivityErrorCode;
import com.potential.goodquestion.common.code.SessionErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.activity.dto.ActivityRequestDto;
import com.potential.goodquestion.domain.activity.dto.ActivityResponseDto;
import com.potential.goodquestion.domain.activity.dto.PostActivityConfig;
import com.potential.goodquestion.domain.activity.entity.Activity;
import com.potential.goodquestion.domain.activity.repository.ActivityRepository;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 말하기 후 활동 서비스
 *
 * 담당 API:
 * - POST  /api/sessions/{sessionId}/activity : 후 활동 시작 (카드 무작위 제시)
 * - PATCH /api/sessions/{sessionId}/activity : 카드 순서 제출·재구성 텍스트 저장·완료 처리
 *
 * 보안:
 * - 세션의 아이가 로그인 보호자 소유인지 검증
 *
 * 정답 여부(is_order_correct)는 프런트엔드가 아니라 서버에서 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final StorySessionRepository storySessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 후 활동 시작 - 카드 제시
     * 세션당 활동은 1건(OneToOne)이므로 이미 있으면 재사용한다.
     *
     * @param sessionId 세션 ID
     * @param parentId  JWT 보호자 ID (소유권 검증)
     * @return 무작위 순서로 제시할 카드 목록
     */
    @Transactional
    public ActivityResponseDto.CardSet startActivity(Long sessionId, Long parentId) {
        StorySession session = getSessionWithOwnerCheck(sessionId, parentId);
        PostActivityConfig config = parseConfig(session.getStory().getPostActivityConfig());

        Activity activity = activityRepository.findBySessionId(sessionId)
                .orElseGet(() -> activityRepository.save(Activity.create(session)));

        List<ActivityResponseDto.CardSet.Card> cards = new ArrayList<>(config.getCards().stream()
                .map(c -> ActivityResponseDto.CardSet.Card.builder()
                        .id(c.getId())
                        .text(c.getText())
                        .build())
                .toList());
        Collections.shuffle(cards);

        return ActivityResponseDto.CardSet.of(activity, cards);
    }

    /**
     * 카드 순서 제출·재구성 텍스트 저장·완료 처리
     *
     * @param sessionId 세션 ID
     * @param parentId  JWT 보호자 ID (소유권 검증)
     * @param request   제출한 카드 순서 + 재구성 텍스트
     * @return 정답 여부 및 (정답 시) 재구성 핵심 단어
     */
    @Transactional
    public ActivityResponseDto.ActivityResult submitActivity(Long sessionId, Long parentId,
            ActivityRequestDto.Submit request) {

        StorySession session = getSessionWithOwnerCheck(sessionId, parentId);
        Activity activity = activityRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));

        PostActivityConfig config = parseConfig(session.getStory().getPostActivityConfig());
        boolean orderCorrect = isOrderCorrect(request.getSubmittedOrder(), config);

        // 카드 순서 제출 (시도 횟수 증가 + 정답 여부 저장)
        activity.submitOrder(toJson(request.getSubmittedOrder()), orderCorrect);
        // 재구성 텍스트 저장 및 완료 처리
        activity.complete(request.getReconstructionText());

        List<String> keywords = orderCorrect ? config.getRetellingKeywords() : List.of();
        return ActivityResponseDto.ActivityResult.of(activity, keywords);
    }

    // ─────────── private ────────────────

    /**
     * 세션 조회 + 보호자 소유권 검증
     */
    private StorySession getSessionWithOwnerCheck(Long sessionId, Long parentId) {
        StorySession session = storySessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        if (!session.getChild().getParent().getId().equals(parentId)) {
            throw new CustomException(SessionErrorCode.SESSION_ACCESS_DENIED);
        }
        return session;
    }

    /**
     * post_activity_config JSON 파싱 (없거나 카드가 비면 예외)
     */
    private PostActivityConfig parseConfig(String json) {
        if (!StringUtils.hasText(json)) {
            throw new CustomException(ActivityErrorCode.POST_ACTIVITY_CONFIG_MISSING);
        }
        try {
            PostActivityConfig config = objectMapper.readValue(json, PostActivityConfig.class);
            if (config.getCards() == null || config.getCards().isEmpty()) {
                throw new CustomException(ActivityErrorCode.POST_ACTIVITY_CONFIG_MISSING);
            }
            return config;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ActivityErrorCode.POST_ACTIVITY_CONFIG_MISSING);
        }
    }

    /**
     * 제출한 카드 순서가 정답(correct_order 오름차순)인지 판정
     * 모든 카드가 정확히 correct_order 순서로 배열되어야 정답.
     */
    private boolean isOrderCorrect(List<String> submittedOrder, PostActivityConfig config) {
        Map<String, Integer> orderById = config.getCards().stream()
                .filter(c -> c.getId() != null && c.getCorrectOrder() != null)
                .collect(Collectors.toMap(PostActivityConfig.Card::getId,
                        PostActivityConfig.Card::getCorrectOrder, (a, b) -> a));

        if (submittedOrder.size() != orderById.size()) {
            return false;
        }
        for (int i = 0; i < submittedOrder.size(); i++) {
            Integer correctOrder = orderById.get(submittedOrder.get(i));
            if (correctOrder == null || correctOrder != i + 1) {
                return false;
            }
        }
        return true;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
