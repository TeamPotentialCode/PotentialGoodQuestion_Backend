package com.potential.goodquestion.domain.child.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import com.potential.goodquestion.domain.parent.entity.Parent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 아이 프로필 엔티티
 */
@Comment("아이 프로필")
@Entity
@Table(name = "children")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Child extends BaseEntity {

    @Comment("아이 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("보호자")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Comment("아이 이름")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Comment("아이 출생연도 (연령은 현재연도 - 출생연도로 계산)")
    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Builder
    public Child(Parent parent, String name, Integer birthYear) {
        this.parent = parent;
        this.name = name;
        this.birthYear = birthYear;
    }

    /**
     * 아이 프로필 생성
     */
    public static Child create(Parent parent, String name, Integer birthYear) {
        return Child.builder()
                .parent(parent)
                .name(name)
                .birthYear(birthYear)
                .build();
    }

    /**
     * 아이 프로필 수정 (이름, 출생연도)
     */
    public void updateProfile(String name, Integer birthYear) {
        this.name = name;
        this.birthYear = birthYear;
    }
}
