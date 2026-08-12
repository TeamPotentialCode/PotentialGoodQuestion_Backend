package com.potential.goodquestion.domain.child.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 아동 개인정보 처리 동의 엔티티
 *
 * 테이블명: child_consents
 *
 * MVP 규칙:
 * - 동의 기록이 없거나 철회된(withdrawn_at != null) 아이는 새 세션 시작 불가
 * - 음성 파일은 저장하지 않으므로 음성 저장 동의는 별도 관리하지 않음
 */
@Comment("아동 개인정보 처리 동의")
@Entity
@Table(name = "child_consents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChildConsent {

    @Comment("동의 기록 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("동의 대상 아이")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Comment("동의한 개인정보 처리 안내문 버전 (ex. mvp_v1)")
    @Column(name = "consent_version", nullable = false, length = 20)
    private String consentVersion;

    @Comment("보호자 확인 방식 (authenticated_parent / institution_paper / mobile_verification)")
    @Column(name = "verification_method", nullable = false, length = 50)
    private String verificationMethod;

    @Comment("동의 일시")
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @Comment("동의 철회 일시 (null이면 유효한 동의)")
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    public ChildConsent(Child child, String consentVersion, String verificationMethod) {
        this.child = child;
        this.consentVersion = consentVersion;
        this.verificationMethod = verificationMethod;
        this.consentedAt = LocalDateTime.now();
    }

    /**
     * 동의 생성 (온라인 보호자 직접 동의)
     */
    public static ChildConsent create(Child child, String consentVersion) {
        return ChildConsent.builder()
                .child(child)
                .consentVersion(consentVersion)
                .verificationMethod("authenticated_parent")
                .build();
    }

    /**
     * 동의 유효 여부 확인 (철회되지 않은 상태)
     */
    public boolean isActive() {
        return this.withdrawnAt == null;
    }

    /**
     * 동의 철회 처리
     */
    public void withdraw() {
        this.withdrawnAt = LocalDateTime.now();
    }
}
