package com.moya.myblogboot.dto.tag;

import com.moya.myblogboot.domain.tag.Tag;

public record TagSummary(String name, String slug) {
    public static TagSummary of(Tag tag) {
        return new TagSummary(tag.getName(), tag.getSlug());
    }
}
