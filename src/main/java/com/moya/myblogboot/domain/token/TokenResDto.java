package com.moya.myblogboot.domain.token;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResDto {
    private String access_token;
    private Long refresh_token_key;
}
