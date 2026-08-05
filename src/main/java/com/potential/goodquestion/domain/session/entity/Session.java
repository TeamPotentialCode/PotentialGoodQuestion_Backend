package com.potential.goodquestion.domain.session.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.session.enums.SessionStatus;
import com.potential.goodquestion.domain.story.entity.Story;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 아이의 이야기 학습 세션 엔티티
 */
@Comment("아이의 이야기 학습 세션")
@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseEntity {

    @Comment("세션 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("학습 아이")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Comment("이야기")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Comment("세션 상태 (IN_PROGRESS: 진행 중, COMPLETED: 완료)")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Comment("세션 완료 일시")
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public Session(Child child, Story story, SessionStatus status) {
        this.child = child;
        this.story = story;
        this.status = status;
    }

    /**
     * 새 세션 생성 (기본 상태: IN_PROGRESS)
     */
    public static Session create(Child child, Story story) {
        return Session.builder()
                .child(child)
                .story(story)
                .status(SessionStatus.IN_PROGRESS)
                .build();
    }

    /**
     * 세션 완료 처리
     */
    public void complete() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
