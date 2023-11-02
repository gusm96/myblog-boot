package com.moya.myblogboot.domain.token;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private String access_token;
    private String refresh_token;
}
