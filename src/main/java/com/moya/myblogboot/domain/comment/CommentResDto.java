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
    public CommentResDto(Comment comment) {
        this.id = comment.getId();
        this.writer = comment.getMember().getNickname();
        this.comment = comment.getComment();
        this.write_date = comment.getWrite_date();
        this.modificationStatus = comment.getModificationStatus();
        this.child = convertToCommentResDto(comment.getChild());

    }

    public static CommentResDto of(Comment comment) {
        return CommentResDto.builder()
                .comment(comment)
                .build();
    }

    private List<CommentResDto> convertToCommentResDto (List<Comment> child){
        return child.stream().map(CommentResDto::of).toList();
    }

}
