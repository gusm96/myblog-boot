package com.moya.myblogboot.domain.event;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class TagChangeEvent extends ApplicationEvent {

    private final String changeType;
    private final Long tagId;
    private final List<String> affectedSlugs;

    public TagChangeEvent(Object source, String changeType, Long tagId, List<String> affectedSlugs) {
        super(source);
        this.changeType = changeType;
        this.tagId = tagId;
        this.affectedSlugs = affectedSlugs == null ? List.of() : List.copyOf(affectedSlugs);
    }

    public String getChangeType() {
        return changeType;
    }

    public Long getTagId() {
        return tagId;
    }

    public List<String> getAffectedSlugs() {
        return affectedSlugs;
    }
}
