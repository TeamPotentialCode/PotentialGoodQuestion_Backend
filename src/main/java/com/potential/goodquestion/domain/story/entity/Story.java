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

    @Comment("이야기 소개 (목록·상세 화면 표시용)")
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Comment("이야기 난이도 (예: 보통)")
    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Comment("주요 주제 JSON 배열 ex) [\"다름\",\"자기이해\",\"장점 발견\"]")
    @Column(name = "topics", columnDefinition = "TEXT")
    private String topics;

    @Comment("대표 이미지 URL")
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Comment("이야기 도입 (배경/줄거리 소개)")
    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Comment("이야기 상황 (장면 배경 설명)")
    @Column(name = "situation", columnDefinition = "TEXT")
    private String situation;

    @Comment("아이 역할 설명")
    @Column(name = "child_role", length = 200)
    private String childRole;

    @Comment("예상 소요 시간 (분 단위)")
    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Comment("말하기 후 활동 설정 JSON (카드 내용, 정답 순서, 핵심 단어)")
    @Column(name = "post_activity_config", columnDefinition = "TEXT")
    private String postActivityConfig;

    @Comment("공개 상태 (published / draft)")
    @Column(name = "status", length = 20, nullable = false)
    private String status = "published";

    @Builder
    public Story(String title, String summary, String difficulty, String topics,
                 String thumbnailUrl, String introduction, String situation,
                 String childRole, Integer estimatedMinutes,
                 String postActivityConfig, String status) {
        this.title = title;
        this.summary = summary;
        this.difficulty = difficulty;
        this.topics = topics;
        this.thumbnailUrl = thumbnailUrl;
        this.introduction = introduction;
        this.situation = situation;
        this.childRole = childRole;
        this.estimatedMinutes = estimatedMinutes;
        this.postActivityConfig = postActivityConfig;
        this.status = status != null ? status : "published";
    }

    public void update(String title, String summary, String difficulty, String topics,
                       String thumbnailUrl, String introduction, String situation,
                       String childRole, Integer estimatedMinutes,
                       String postActivityConfig, String status) {
        this.title = title;
        this.summary = summary;
        this.difficulty = difficulty;
        this.topics = topics;
        this.thumbnailUrl = thumbnailUrl;
        this.introduction = introduction;
        this.situation = situation;
        this.childRole = childRole;
        this.estimatedMinutes = estimatedMinutes;
        this.postActivityConfig = postActivityConfig;
        this.status = status != null ? status : "published";
    }
}
