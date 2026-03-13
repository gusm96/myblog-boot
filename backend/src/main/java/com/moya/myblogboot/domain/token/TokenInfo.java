package com.moya.myblogboot.domain.token;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {
    private Long memberPrimaryKey;
    private String role;
}
