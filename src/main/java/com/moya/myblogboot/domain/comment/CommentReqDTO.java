package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.board.Board;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReqDTO {
    @NotBlank(message = "작성자 이름은 2~6자 숫자,문자로 이루어져야합니다.")
    @Size(min = 2,max = 6)
    private String writer;
    @NotBlank(message = "댓글은 2글자 이상 500글자 이하로 작성하여야합니다.")
    @Size(min = 2, max = 500)
    private String comment;
    private CommentType commentType;
    private Long board_id;
    private Long parent_id;

    public Comment toEntity (Board board){
        return Comment.builder()
                .writer(this.writer)
                .comment(this.comment)
                .commentType(this.commentType)
                .board(board)
                .build();
    }

}

