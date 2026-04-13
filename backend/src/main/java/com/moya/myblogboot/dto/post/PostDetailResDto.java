package com.moya.myblogboot.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moya.myblogboot.domain.post.PostStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDetailResDto {

    private Long id;
    private String title;
    private String content;
    private Long views;
    private Long likes;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private PostStatus postStatus;
    private String slug;
    private String metaDescription;
    private String thumbnailUrl;
    private String categoryName;

    @Builder
    public PostDetailResDto(PostForRedis postForRedis) {
        this.id = postForRedis.getId();
        this.title = postForRedis.getTitle();
        this.content = postForRedis.getContent();
        this.views = postForRedis.totalViews();
        this.likes = postForRedis.totalLikes();
        this.createDate = postForRedis.getCreateDate();
        this.updateDate = postForRedis.getUpdateDate();
        this.deleteDate = postForRedis.getDeleteDate();
        this.postStatus = postForRedis.getPostStatus();
        this.slug = postForRedis.getSlug();
        this.metaDescription = postForRedis.getMetaDescription();
        this.thumbnailUrl = postForRedis.getThumbnailUrl();
        this.categoryName = postForRedis.getCategoryName();
    }
}
