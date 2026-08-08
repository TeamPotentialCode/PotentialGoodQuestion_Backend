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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 말하기 후 활동 엔티티 (이야기 재구성)
 *
 * 테이블명: post_activity_results
 */
@Comment("말하기 후 활동 (이야기 재구성)")
@Entity
@Table(name = "post_activity_results")
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

    @Comment("아이가 최종 제출한 카드 순서 (JSON 배열 ex. [\"card_2\",\"card_1\",\"card_3\"])")
    @Column(name = "submitted_order", columnDefinition = "TEXT")
    private String submittedOrder;

    @Comment("카드 배열 정답 여부")
    @Column(name = "is_order_correct")
    private Boolean isOrderCorrect;

    @Comment("순서 배열 시도 횟수")
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Comment("핵심 단어를 사용해 아이가 다시 말한 이야기 (STT 변환 결과)")
    @Column(name = "retelling_text", columnDefinition = "TEXT")
    private String retellingText;

    @Comment("말하기 후 활동 완료 일시")
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public Activity(StorySession session) {
        this.session = session;
        this.attemptCount = 0;
    }

    /**
     * 활동 생성 (미완료 상태로 시작)
     */
    public static Activity create(StorySession session) {
        return Activity.builder()
                .session(session)
                .build();
    }

    /**
     * 카드 순서 제출 처리 — 시도 횟수 증가 및 정답 여부 저장
     *
     * @param submittedOrder  아이가 제출한 카드 순서 JSON
     * @param isOrderCorrect  정답 여부 (서버에서 계산)
     */
    public void submitOrder(String submittedOrder, boolean isOrderCorrect) {
        this.submittedOrder = submittedOrder;
        this.isOrderCorrect = isOrderCorrect;
        this.attemptCount++;
    }

    /**
     * 활동 완료 처리 — 이야기 재구성 텍스트 저장 및 완료 일시 기록
     *
     * @param retellingText  STT 변환된 재구성 발화 텍스트
     */
    public void complete(String retellingText) {
        this.retellingText = retellingText;
        this.completedAt = LocalDateTime.now();
    }
}
