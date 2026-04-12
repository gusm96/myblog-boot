package com.moya.myblogboot.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentReqDto {

    @Size(min = 1, max = 10)
    private String nickname;   // 비회원 필수, 어드민 불필요

    @Size(min = 4, max = 20)
    private String password;   // 비회원 필수, 어드민 불필요

    @NotBlank(message = "댓글은 2글자 이상 500글자 이하로 작성하여야 합니다.")
    @Size(min = 2, max = 500)
    private String comment;

    private Long parentId;
}
