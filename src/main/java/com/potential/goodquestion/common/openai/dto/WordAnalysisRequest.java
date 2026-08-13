package com.potential.goodquestion.common.openai.dto;

/**
 * 단어 뜻·예시 문장 생성 GPT 요청 DTO
 *
 * @param word            저장할 단어
 * @param contextSentence 이야기 속 원문 문장 (단어가 사용된 문장)
 * @param childAge        아이 나이 (GPT가 눈높이를 맞추는 데 사용)
 */
public record WordAnalysisRequest(
        String word,
        String contextSentence,
        int childAge
) {
}
