package com.potential.goodquestion.domain.activity.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.storysession.entity.StorySession;
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

/**
 * 말하기 후 활동 엔티티 (이야기 재구성)
 */
@Comment("말하기 후 활동 (이야기 재구성)")
@Entity
@Table(name = "activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends BaseEntity {

    @Comment("활동 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("연결된 세션")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private StorySession session;

    @Comment("장면 카드 배열 순서 (JSON 형태로 저장)")
    @Column(name = "card_order", columnDefinition = "TEXT")
    private String cardOrder;

    @Comment("전체 이야기 음성 재구성 텍스트 (STT 변환 결과)")
    @Column(name = "reconstruction_text", columnDefinition = "TEXT")
    private String reconstructionText;

    @Comment("카드 배열 정답 여부 (서버 계산, 미제출 시 null)")
    @Column(name = "is_order_correct")
    private Boolean isOrderCorrect;

    @Comment("활동 완료 여부")
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    @Builder
    public Activity(StorySession session, String cardOrder, String reconstructionText,
                    Boolean isOrderCorrect, Boolean isCompleted) {
        this.session = session;
        this.cardOrder = cardOrder;
        this.reconstructionText = reconstructionText;
        this.isOrderCorrect = isOrderCorrect;
        this.isCompleted = isCompleted;
    }

    /**
     * 활동 생성 (미완료 상태로 시작)
     */
    public static Activity create(StorySession session) {
        return Activity.builder()
                .session(session)
                .isCompleted(false)
                .build();
    }

    /**
     * 활동 완료 처리 (카드 순서, 정답 여부, 재구성 텍스트 저장)
     */
    public void complete(String cardOrder, Boolean isOrderCorrect, String reconstructionText) {
        this.cardOrder = cardOrder;
        this.isOrderCorrect = isOrderCorrect;
        this.reconstructionText = reconstructionText;
        this.isCompleted = true;
    }
}
