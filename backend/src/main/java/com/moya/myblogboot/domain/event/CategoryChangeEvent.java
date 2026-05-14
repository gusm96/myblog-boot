package com.moya.myblogboot.domain.event;

import org.springframework.context.ApplicationEvent;

public class CategoryChangeEvent extends ApplicationEvent {

    private final String changeType;
    private final Long categoryId;

    public CategoryChangeEvent(Object source, String changeType, Long categoryId) {
        super(source);
        this.changeType = changeType;
        this.categoryId = categoryId;
    }

    public String getChangeType() { return changeType; }
    public Long getCategoryId() { return categoryId; }
}
