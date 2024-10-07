package com.moya.myblogboot.dto.member;


import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RandomUserNumberDto {
    private long number;
    private long expireTime;
}
