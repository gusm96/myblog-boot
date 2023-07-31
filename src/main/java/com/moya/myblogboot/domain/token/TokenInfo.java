package com.moya.myblogboot.domain.token;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenInfo {
    private String name;
    private TokenUserType type;

    @Builder
    public TokenInfo(String name, TokenUserType type) {
        this.name = name;
        this.type = type;
    }
}
