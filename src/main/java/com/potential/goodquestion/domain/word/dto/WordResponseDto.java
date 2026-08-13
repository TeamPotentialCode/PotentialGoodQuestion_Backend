package com.potential.goodquestion.domain.word.dto;

import com.potential.goodquestion.domain.word.entity.Word;
import com.potential.goodquestion.domain.word.enums.WordSource;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 단어장 응답 DTO 모음
 */
public class WordResponseDto {

    /**
     * 단어 단건 응답
     * 저장/즐겨찾기/조회 시 공통 사용
     */
    @Getter
    @Builder
    public static class WordInfo {

        private Long wordId;
        private String word;
        private String contextSentence;  // 이야기 속 원문 문장
        private String meaning;          // GPT 생성 쉬운 뜻
        private String exampleSentence;  // GPT 생성 예시 문장
        private WordSource source;       // 저장 출처 (CHILD / PARENT)
        private boolean favorite;        // 즐겨찾기 여부
        private boolean learned;         // 학습 완료 여부 (0: 미완료, 1: 완료)
        private LocalDateTime createdAt;

        /**
         * Word 엔티티 → WordInfo 변환
         */
        public static WordInfo from(Word word) {
            return WordInfo.builder()
                    .wordId(word.getId())
                    .word(word.getWord())
                    .contextSentence(word.getContextSentence())
                    .meaning(word.getMeaning())
                    .exampleSentence(word.getExampleSentence())
                    .source(word.getSource())
                    .favorite(word.isFavorite())
                    .learned(word.isLearned())
                    .createdAt(word.getCreatedAt())
                    .build();
        }
    }

    /**
     * 단어장 목록 응답
     * GET /api/children/{childId}/words
     */
    @Getter
    @Builder
    public static class WordList {

        private int totalCount;
        private int favoriteCount;
        private List<WordInfo> words;

        public static WordList from(List<Word> words) {
            List<WordInfo> wordInfos = words.stream()
                    .map(WordInfo::from)
                    .toList();
            long favoriteCount = words.stream().filter(Word::isFavorite).count();
            return WordList.builder()
                    .totalCount(words.size())
                    .favoriteCount((int) favoriteCount)
                    .words(wordInfos)
                    .build();
        }
    }
}
