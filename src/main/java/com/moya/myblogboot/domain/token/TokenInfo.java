package com.moya.myblogboot.domain.token;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenInfo {
    private String name;
    private String role;

    @Builder
    public TokenInfo(String name, String role) {
        this.name = name;
        this.role = role;
    }
}
