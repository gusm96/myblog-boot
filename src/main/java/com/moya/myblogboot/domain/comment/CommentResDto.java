package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.board.ModificationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentResDto {
    private Long id;
    private String writer;
    private String comment;
    private LocalDateTime write_date;
    private ModificationStatus modificationStatus;
    private List<CommentResDto> child;

    @Builder
    public CommentResDto(Comment comment , List<CommentResDto> child) {
        this.id = comment.getId();
        this.writer = comment.getMember().getNickname();
        this.comment = comment.getComment();
        this.write_date = comment.getWrite_date();
        this.modificationStatus = comment.getModificationStatus();
        this.child = child;
    }
    @Builder
    public CommentResDto(Comment comment) {
        this.id = comment.getId();
        this.writer = comment.getMember().getNickname();
        this.comment = comment.getComment();
        this.write_date = comment.getWrite_date();
        this.modificationStatus = comment.getModificationStatus();
    }

    public static CommentResDto of(Comment comment) {
        return CommentResDto.builder()
                .comment(comment)
                .build();
    }


}
