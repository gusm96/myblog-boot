package com.moya.myblogboot.domain.token;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenReqDto {
    private Long refreshTokenKey;
}
