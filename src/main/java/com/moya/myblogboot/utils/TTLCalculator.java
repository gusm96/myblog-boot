package com.moya.myblogboot.utils;


import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/*
* Redis 와 Cookie를 사용하면서 만료 시간을 계산하는 것을 조금 더 편하게 하기 위해
* Time To Live 계산기를 간단하게 만들어보았습니다.
* */
public class TTLCalculator {
   public static long calculateTTL(int value, ChronoUnit chronoUnit){
       Instant now = Instant.now();
       Instant future = now.plus(value, chronoUnit);
       return ChronoUnit.SECONDS.between(now, future);
   }

    public static long calculateSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).getSeconds();
    }
}
