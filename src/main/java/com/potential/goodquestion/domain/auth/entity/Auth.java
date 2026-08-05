package com.potential.goodquestion.domain.auth.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.parent.entity.Parent;
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
 * 인증 토큰 엔티티 (Refresh Token 관리)
 */
@Comment("인증 토큰 (Refresh Token 관리)")
@Entity
@Table(name = "auth_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth extends BaseEntity {

    @Comment("토큰 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("보호자")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false, unique = true)
    private Parent parent;

    @Comment("Refresh Token 값")
    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @Builder
    public Auth(Parent parent, String refreshToken) {
        this.parent = parent;
        this.refreshToken = refreshToken;
    }

    /**
     * Auth 토큰 생성
     */
    public static Auth create(Parent parent, String refreshToken) {
        return Auth.builder()
                .parent(parent)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Refresh Token 갱신
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
