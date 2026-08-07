package com.potential.goodquestion.domain.child.dto;

import com.potential.goodquestion.domain.child.entity.Child;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 아이 프로필 응답 DTO 모음
 */
public class ChildResponseDto {

    /**
     * 아이 프로필 단건 응답
     * 등록/수정/단건 조회 시 공통 사용
     */
    @Getter
    @Builder
    public static class ChildInfo {

        private Long childId;
        private String name;
        private Integer age;
        private LocalDateTime createdAt;

        /**
         * Child 엔티티 → ChildInfo 변환
         */
        public static ChildInfo from(Child child) {
            return ChildInfo.builder()
                    .childId(child.getId())
                    .name(child.getName())
                    .age(child.getAge())
                    .createdAt(child.getCreatedAt())
                    .build();
        }
    }
}
