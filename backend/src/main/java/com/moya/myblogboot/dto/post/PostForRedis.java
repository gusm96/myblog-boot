package com.moya.myblogboot.dto.post;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.dto.tag.TagSummary;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostForRedis {

    private Long id;
    private String title;
    private String content;
    private Long views;
    private Long updateViews;
    private Long likes;
    private Long updateLikes;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
    private PostStatus postStatus;
    private String slug;
    private String metaDescription;
    private String thumbnailUrl;
    private List<TagSummary> tags;

    @Builder
    public PostForRedis(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.views = post.getViews();
        this.updateViews = 0L;
        this.likes = post.getLikes() == null ? 0L : post.getLikes();
        this.updateLikes = 0L;
        this.createDate = post.getCreateDate();
        this.updateDate = post.getUpdateDate();
        this.deleteDate = post.getDeleteDate();
        this.postStatus = post.getPostStatus();
        this.slug = post.getSlug();
        this.metaDescription = post.getMetaDescription();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.tags = post.getTags().stream().map(TagSummary::of).toList();
    }

    public void incrementViews() {
        this.views++;
    }

    public void setUpdateViews(Long updateViews) {
        this.updateViews = updateViews;
    }

    public void setUpdateLikes(Long updateLikes) {
        this.updateLikes = updateLikes;
    }

    public Long totalViews() {
        return this.views + this.updateViews;
    }

    public Long totalLikes() {
        return this.likes + this.updateLikes;
    }

    public void update(Post post) {
        this.title = post.getTitle();
        this.content = post.getContent();
        this.updateDate = post.getUpdateDate();
        this.deleteDate = post.getDeleteDate();
        this.postStatus = post.getPostStatus();
        this.slug = post.getSlug();
        this.metaDescription = post.getMetaDescription();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.tags = post.getTags().stream().map(TagSummary::of).toList();
    }
}
