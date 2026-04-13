package com.moya.myblogboot.dto.comment;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class CommentWriteResDto {

    private String nickname;
    private String discriminator;
}
