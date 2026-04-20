package com.moya.myblogboot.domain.event;

import org.springframework.context.ApplicationEvent;

public class PostChangeEvent extends ApplicationEvent {

    private final String changeType;
    private final Long postId;
    private final String slug;

    public PostChangeEvent(Object source, String changeType, Long postId, String slug) {
        super(source);
        this.changeType = changeType;
        this.postId = postId;
        this.slug = slug;
    }

    // 하위 호환 — SseEmitterService 기존 호출 영향 없음
    public PostChangeEvent(Object source, String changeType, Long postId) {
        this(source, changeType, postId, null);
    }

    public String getChangeType() { return changeType; }
    public Long getPostId() { return postId; }
    public String getSlug() { return slug; }
}
