package com.potential.goodquestion.domain.word.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.word.enums.WordSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 아이 단어장 엔티티
 * 아이가 이야기 대화 중 또는 보호자가 리포트에서 저장한 단어를 보관한다.
 */
@Comment("아이 단어장")
@Entity
@Table(name = "word_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word extends BaseEntity {

    @Comment("단어 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("아이")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Comment("저장된 단어")
    @Column(name = "word", nullable = false, length = 100)
    private String word;

    @Comment("이야기 속 원문 문장 (단어가 등장한 문장)")
    @Column(name = "context_sentence", columnDefinition = "TEXT")
    private String contextSentence;

    @Comment("GPT 생성 — 아이 눈높이 쉬운 뜻 (1~2문장)")
    @Column(name = "meaning", columnDefinition = "TEXT")
    private String meaning;

    @Comment("GPT 생성 — 단어를 활용한 예시 문장 (1문장)")
    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Comment("저장 출처 (CHILD: 아이가 대화 중 / PARENT: 보호자가 리포트에서)")
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private WordSource source;

    @Comment("즐겨찾기 여부")
    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @Comment("학습 완료 여부 (아이가 단어 카드를 보고 완료 처리)")
    @Column(name = "is_learned", nullable = false)
    private boolean learned;

    @Builder
    public Word(Child child, String word, String contextSentence,
            String meaning, String exampleSentence, WordSource source) {
        this.child = child;
        this.word = word;
        this.contextSentence = contextSentence;
        this.meaning = meaning;
        this.exampleSentence = exampleSentence;
        this.source = source;
        this.favorite = false;
        this.learned = false;
    }

    /**
     * 단어장 항목 생성
     */
    public static Word create(Child child, String word, String contextSentence,
            String meaning, String exampleSentence, WordSource source) {
        return Word.builder()
                .child(child)
                .word(word)
                .contextSentence(contextSentence)
                .meaning(meaning)
                .exampleSentence(exampleSentence)
                .source(source)
                .build();
    }

    /**
     * 즐겨찾기 상태 토글
     */
    public void toggleFavorite() {
        this.favorite = !this.favorite;
    }

    /**
     * 학습 완료 상태 토글
     */
    public void toggleLearned() {
        this.learned = !this.learned;
    }
}
