package com.moya.myblogboot.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyReqDTO {
    @NotBlank(message = "작성자 이름은 2~6자 숫자,문자로 이루어져야합니다.")
    @Size(min = 2,max = 6)
    private String writer;
    @NotBlank(message = "비밀번호는 4~8자 숫자,문자로 이루어져야합니다.(공백, 특수문자 입력 X")
    @Size(min = 4, max = 8)
    private String password;
    @NotBlank(message = "댓글은 2글자 이상 500글자 이하로 작성하여야합니다.")
    @Size(min = 2, max = 500)
    private String comment;
    private Long board_id;
    private Long parent_id;

    public Reply toEntity (Board board){
        return Reply.builder()
                .writer(this.writer)
                .password(this.password)
                .comment(this.comment)
                .board(board)
                .build();
    }
}

