package com.moya.myblogboot.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostResDto {
    private Long id;
    private String title;
    private String content;
    private String slug;
    private String thumbnailUrl;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private PostStatus postStatus;

    @Builder
    public PostResDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.slug = post.getSlug();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.createDate = post.getCreateDate();
        this.updateDate = post.getUpdateDate();
        this.deleteDate = post.getDeleteDate();
        this.postStatus = post.getPostStatus();
    }

    public static PostResDto of(Post post) {
        return PostResDto.builder()
                .post(post)
                .build();
    }
}
