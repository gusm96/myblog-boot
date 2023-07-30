package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.board.ModificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentResDto {
    private Long id;
    private Long boardId;
    private Long parentId;
    private String writer;
    private String comment;
    private LocalDateTime write_date;
    private ModificationStatus modificationStatus;

    @Builder
    public CommentResDto(Comment comment) {
        this.id = comment.getId();
        this.boardId = comment.getBoard().getId();
        if(comment.getParent() != null){
            this.parentId = comment.getParent().getId();
        }
        this.writer = comment.getWriter();
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
