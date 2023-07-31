package com.moya.myblogboot.domain.token;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    private String access_token;
    private String refresh_token;

}
