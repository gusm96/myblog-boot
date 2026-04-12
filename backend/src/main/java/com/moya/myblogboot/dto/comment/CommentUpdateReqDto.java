package com.moya.myblogboot.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateReqDto {

    private String password;

    @NotBlank(message = "댓글은 2글자 이상 500글자 이하로 작성하여야 합니다.")
    @Size(min = 2, max = 500)
    private String comment;
}
