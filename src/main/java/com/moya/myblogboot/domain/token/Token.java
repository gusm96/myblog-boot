package com.moya.myblogboot.domain.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Token {
    private String access_token;
    private String refresh_token;

    @Builder
    public Token (String access_token, String refresh_token){
        this.access_token = access_token;
        this.refresh_token = refresh_token;
    }
}
