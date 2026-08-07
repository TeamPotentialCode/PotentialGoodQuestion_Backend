package com.potential.goodquestion.domain.child.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이 프로필 요청 DTO 모음
 */
public class ChildRequestDto {

    /**
     * 아이 등록 요청 (POST /api/children)
     */
    @Getter
    @NoArgsConstructor
    public static class Create {

        @NotBlank(message = "아이 이름은 필수입니다.")
        @Size(max = 50, message = "아이 이름은 50자 이하여야 합니다.")
        private String name;

        @NotNull(message = "아이 나이는 필수입니다.")
        @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
        @Max(value = 20, message = "나이는 20 이하여야 합니다.")
        private Integer age;
    }

    /**
     * 아이 프로필 수정 요청 (PATCH /api/children/{childId})
     */
    @Getter
    @NoArgsConstructor
    public static class Update {

        @NotBlank(message = "아이 이름은 필수입니다.")
        @Size(max = 50, message = "아이 이름은 50자 이하여야 합니다.")
        private String name;

        @NotNull(message = "아이 나이는 필수입니다.")
        @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
        @Max(value = 20, message = "나이는 20 이하여야 합니다.")
        private Integer age;
    }
}
