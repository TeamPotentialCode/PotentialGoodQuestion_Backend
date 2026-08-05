package com.potential.goodquestion.domain.parent.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 보호자 계정 엔티티
 */
@Comment("보호자 계정")
@Entity
@Table(name = "parents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parent extends BaseEntity {

    @Comment("보호자 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("이메일")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Comment("이름")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Comment("소셜 로그인 제공자 (GOOGLE, NAVER)")
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Comment("소셜 로그인 제공자 사용자 ID")
    @Column(name = "provider_id", nullable = false, length = 200)
    private String providerId;

    @Comment("등록된 아이 프로필 목록")
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Child> children = new ArrayList<>();

    @Builder
    public Parent(String email, String name, OAuthProvider provider, String providerId) {
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * 보호자 계정 생성
     */
    public static Parent create(String email, String name, OAuthProvider provider, String providerId) {
        return Parent.builder()
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    /**
     * 이름 수정
     */
    public void updateName(String name) {
        this.name = name;
    }
}
