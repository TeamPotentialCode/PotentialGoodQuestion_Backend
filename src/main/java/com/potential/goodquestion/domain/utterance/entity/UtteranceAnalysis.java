package com.potential.goodquestion.domain.utterance.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.message.entity.Message;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Comment("아이 발화 분석 결과")
@Entity
@Table(name = "utterance_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtteranceAnalysis extends BaseEntity {

    @Comment("분석 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("연결된 메시지 (1:1)")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @Comment("발화 의도 코드")
    @Column(name = "child_intent", length = 30)
    private String childIntent;

    @Comment("발화 핵심 뜻 (null 가능)")
    @Column(name = "main_point", columnDefinition = "TEXT")
    private String mainPoint;

    @Comment("탐지된 사고 요소 JSON 배열 ex) [{\"type\":\"REASON\",\"evidence\":\"...\"}]")
    @Column(name = "detected_elements", columnDefinition = "TEXT")
    private String detectedElements;

    @Comment("발화 유효성 (VALID / SHORT / UNCLEAR / OFF_TOPIC / PLAYFUL)")
    @Column(name = "utterance_validity", length = 20)
    private String utteranceValidity;

    @Builder
    public UtteranceAnalysis(Message message, String childIntent, String mainPoint,
                             String detectedElements, String utteranceValidity) {
        this.message = message;
        this.childIntent = childIntent;
        this.mainPoint = mainPoint;
        this.detectedElements = detectedElements;
        this.utteranceValidity = utteranceValidity;
    }
}
