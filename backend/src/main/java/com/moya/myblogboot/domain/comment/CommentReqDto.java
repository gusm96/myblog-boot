package com.moya.myblogboot.domain.comment;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.member.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentReqDto {

    @NotBlank(message = "댓글은 2글자 이상 500글자 이하로 작성하여야합니다.")
    @Size(min = 2, max = 500)
    private String comment;
    private Long parentId;

    public Comment toEntity (Member member, Board board){
        return Comment.builder()
                .comment(this.comment)
                .member(member)
                .board(board)
                .build();
    }

}

