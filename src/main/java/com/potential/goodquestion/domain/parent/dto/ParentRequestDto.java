package com.potential.goodquestion.domain.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ParentRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Update {

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        private String name;
    }
}
