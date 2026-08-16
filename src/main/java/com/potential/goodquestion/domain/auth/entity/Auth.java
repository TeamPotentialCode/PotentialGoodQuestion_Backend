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

    @Comment("소셜 provider Access Token (Google/Naver unlink 시 사용, LOCAL은 null)")
    @Column(name = "oauth_access_token", length = 2000)
    private String oauthAccessToken;

    @Builder
    public Auth(Parent parent, String refreshToken, String oauthAccessToken) {
        this.parent = parent;
        this.refreshToken = refreshToken;
        this.oauthAccessToken = oauthAccessToken;
    }

    /** LOCAL 로그인용 생성 */
    public static Auth create(Parent parent, String refreshToken) {
        return Auth.builder()
                .parent(parent)
                .refreshToken(refreshToken)
                .build();
    }

    /** 소셜 로그인용 생성 (provider access token 포함) */
    public static Auth createOAuth(Parent parent, String refreshToken, String oauthAccessToken) {
        return Auth.builder()
                .parent(parent)
                .refreshToken(refreshToken)
                .oauthAccessToken(oauthAccessToken)
                .build();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateOauthToken(String oauthAccessToken) {
        this.oauthAccessToken = oauthAccessToken;
    }
}
