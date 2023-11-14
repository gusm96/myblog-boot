package com.moya.myblogboot.domain.member;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PwStrengthCheckReqDto {
    private String password;
}
