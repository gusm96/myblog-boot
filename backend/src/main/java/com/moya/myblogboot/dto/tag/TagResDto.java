package com.moya.myblogboot.dto.tag;

import com.moya.myblogboot.domain.tag.Tag;
import lombok.Builder;
import lombok.Getter;

@Getter
public class TagResDto {
    private final Long id;
    private final String name;
    private final String slug;
    private final int postCount;

    @Builder
    public TagResDto(Long id, String name, String slug, int postCount) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.postCount = postCount;
    }

    public static TagResDto of(Tag tag) {
        return TagResDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .postCount(tag.getPostCount())
                .build();
    }
}
