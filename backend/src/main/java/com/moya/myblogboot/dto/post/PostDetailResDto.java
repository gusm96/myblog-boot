package com.moya.myblogboot.dto.post;

import com.moya.myblogboot.domain.post.PostStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
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
    }
}
