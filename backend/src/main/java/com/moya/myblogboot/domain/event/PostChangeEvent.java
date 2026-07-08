package com.moya.myblogboot.domain.event;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class PostChangeEvent extends ApplicationEvent {

    private final String changeType;
    private final Long postId;
    private final String slug;
    private final List<String> tagSlugs;

    public PostChangeEvent(Object source, String changeType, Long postId, String slug, List<String> tagSlugs) {
        super(source);
        this.changeType = changeType;
        this.postId = postId;
        this.slug = slug;
        this.tagSlugs = tagSlugs == null ? List.of() : List.copyOf(tagSlugs);
    }

    public PostChangeEvent(Object source, String changeType, Long postId, String slug) {
        this(source, changeType, postId, slug, List.of());
    }

    // 하위 호환 — SseEmitterService 기존 호출 영향 없음
    public PostChangeEvent(Object source, String changeType, Long postId) {
        this(source, changeType, postId, null);
    }

    public String getChangeType() { return changeType; }
    public Long getPostId() { return postId; }
    public String getSlug() { return slug; }
    public List<String> getTagSlugs() { return tagSlugs; }
}
