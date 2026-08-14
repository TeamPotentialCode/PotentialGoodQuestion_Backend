package com.potential.goodquestion.common.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 이야기 · 장면 조회 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum StoryErrorCode implements ErrorCode {

    // 이야기 조회
    STORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STORY_001", "이야기를 찾을 수 없습니다."),

    // 장면 조회 (없거나 해당 이야기에 속하지 않는 경우 모두 포함)
    SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORY_002", "장면을 찾을 수 없습니다."),

    // 전개·도입 장면이 아닌데 내레이션 완료 요청한 경우
    NOT_NARRATION_SCENE(HttpStatus.BAD_REQUEST, "STORY_003", "내레이션 장면이 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
