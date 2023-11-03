package com.moya.myblogboot.domain.token;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {
    private String username;
    private String role;
}
