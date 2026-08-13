package com.potential.goodquestion.common.openai.dto;

/**
 * 단어 뜻·예시 문장 생성 GPT 응답 DTO
 *
 * @param meaning         아이 눈높이에 맞춘 쉬운 뜻 (1~2문장)
 * @param exampleSentence 단어를 활용한 예시 문장 (1문장)
 */
public record WordAnalysisResponse(
        String meaning,
        String exampleSentence
) {
}
