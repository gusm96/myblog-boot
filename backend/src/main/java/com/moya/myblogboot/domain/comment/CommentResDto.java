package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.post.ModificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentResDto {
    private Long id;
    private String writer;    // "nickname#discriminator" 또는 "[관리자]"
    private Boolean isAdmin;
    private String comment;
    private LocalDateTime createDate;
    private ModificationStatus modificationStatus;
    private Long childCount = 0L;

    @Builder
    public CommentResDto(Comment comment) {
        this.id = comment.getId();
        this.writer = Boolean.TRUE.equals(comment.getIsAdmin())
                ? "[관리자]"
                : comment.getNickname() + "#" + comment.getDiscriminator();
        this.isAdmin = comment.getIsAdmin();
        this.comment = comment.getComment();
        this.createDate = comment.getCreateDate();
        this.modificationStatus = comment.getModificationStatus();
        this.childCount = (long) comment.getChild().size();
    }

    public static CommentResDto of(Comment comment) {
        return CommentResDto.builder()
                .comment(comment)
                .build();
    }
}
