package com.moya.myblogboot.domain.event;

import org.springframework.context.ApplicationEvent;

public class PostChangeEvent extends ApplicationEvent {

    private final String changeType;
    private final Long postId;

    public PostChangeEvent(Object source, String changeType, Long postId) {
        super(source);
        this.changeType = changeType;
        this.postId = postId;
    }

    public String getChangeType() {
        return changeType;
    }

    public Long getPostId() {
        return postId;
    }
}
