package com.potential.goodquestion.domain.story.entity;

import com.potential.goodquestion.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 이야기 콘텐츠 엔티티
 */
@Comment("이야기 콘텐츠")
@Entity
@Table(name = "stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story extends BaseEntity {

    @Comment("이야기 ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("이야기 제목")
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Comment("대표 이미지 URL")
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Comment("이야기 도입 (배경/줄거리 소개)")
    @Column(name = "introduction", nullable = false, columnDefinition = "TEXT")
    private String introduction;

    @Comment("이야기 상황 (장면 배경 설명)")
    @Column(name = "situation", nullable = false, columnDefinition = "TEXT")
    private String situation;

    @Comment("아이 역할 설명")
    @Column(name = "child_role", nullable = false, length = 200)
    private String childRole;

    @Comment("예상 소요 시간 (분 단위)")
    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Comment("주제 (예: 우정, 용기, 배려)")
    @Column(name = "topic", nullable = false, length = 50)
    private String topic;

    @Builder
    public Story(String title, String thumbnailUrl, String introduction,
                 String situation, String childRole, Integer estimatedMinutes, String topic) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.introduction = introduction;
        this.situation = situation;
        this.childRole = childRole;
        this.estimatedMinutes = estimatedMinutes;
        this.topic = topic;
    }

    /**
     * 이야기 콘텐츠 생성
     */
    public static Story create(String title, String thumbnailUrl, String introduction,
                               String situation, String childRole,
                               Integer estimatedMinutes, String topic) {
        return Story.builder()
                .title(title)
                .thumbnailUrl(thumbnailUrl)
                .introduction(introduction)
                .situation(situation)
                .childRole(childRole)
                .estimatedMinutes(estimatedMinutes)
                .topic(topic)
                .build();
    }
}
