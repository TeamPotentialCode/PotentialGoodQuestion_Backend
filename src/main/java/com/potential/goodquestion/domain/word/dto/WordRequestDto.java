package com.potential.goodquestion.domain.word.dto;

import com.potential.goodquestion.domain.word.enums.WordSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 단어장 요청 DTO 모음
 */
public class WordRequestDto {

    /**
     * 단어 저장 요청 (POST /api/children/{childId}/words)
     */
    @Getter
    @NoArgsConstructor
    public static class Save {

        @NotBlank(message = "단어는 필수입니다.")
        @Size(max = 100, message = "단어는 100자 이하여야 합니다.")
        private String word;

        @Size(max = 500, message = "원문 문장은 500자 이하여야 합니다.")
        private String contextSentence;

        @NotNull(message = "저장 출처(source)는 필수입니다.")
        private WordSource source;
    }
}
