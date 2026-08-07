package com.potential.goodquestion.domain.message.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.common.enums.SpeakerType;
import com.potential.goodquestion.domain.scene.entity.StoryScene;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
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

@Comment("대화 메시지")
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Comment("메시지 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("세션")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StorySession session;

    @Comment("장면")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    private StoryScene scene;

    @Comment("발화자 (CHILD / CHARACTER)")
    @Enumerated(EnumType.STRING)
    @Column(name = "speaker_type", length = 20, nullable = false)
    private SpeakerType speakerType;

    @Comment("대화 내용")
    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Comment("STT 원본 텍스트 (아이 발화만 해당)")
    @Column(name = "stt_raw_text", columnDefinition = "TEXT")
    private String sttRawText;

    @Builder
    public Message(StorySession session, StoryScene scene, SpeakerType speakerType,
                   String text, String sttRawText) {
        this.session = session;
        this.scene = scene;
        this.speakerType = speakerType;
        this.text = text;
        this.sttRawText = sttRawText;
    }

    public static Message ofChild(StorySession session, StoryScene scene,
                                  String text, String sttRawText) {
        return new Message(session, scene, SpeakerType.CHILD, text, sttRawText);
    }

    public static Message ofCharacter(StorySession session, StoryScene scene, String text) {
        return new Message(session, scene, SpeakerType.CHARACTER, text, null);
    }
}
