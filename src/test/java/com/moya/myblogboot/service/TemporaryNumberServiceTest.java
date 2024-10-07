package com.moya.myblogboot.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
public class TemporaryNumberServiceTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
    // Redis Store에 중복된 임시번호가 존재하면 안된다.
    // TTL은 24시 - 현재시간이다. 매일 자정에 초기화 할 것이기 때문.

    @Test
    @DisplayName("임시번호 생성")
    void createTemporaryNumber() {
        long temporaryNumber = generateUniqueTemporaryNumber();

        String formattedNow = getCurrentFormattedTime();
        long ttl = calculateTTL();

        saveTemporaryNumber(temporaryNumber, formattedNow, ttl);
    }

    // 중복되지 않는 임시번호 생성
    private long generateUniqueTemporaryNumber() {
        long temporaryNumber;
        do {
            temporaryNumber = ThreadLocalRandom.current().nextLong(); // 난수 생성
        } while (isNumberInRedis(temporaryNumber));
        return temporaryNumber;
    }

    private boolean isNumberInRedis(long number) {
        return redisTemplate.opsForValue().get(String.valueOf(number)) != null;
    }

    private String getCurrentFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(FORMATTER);
    }

    private long calculateTTL() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).truncatedTo(ChronoUnit.DAYS);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    private void saveTemporaryNumber(long number, String formattedNow, long ttl) {
        redisTemplate.opsForValue().set(String.valueOf(number), formattedNow, ttl, TimeUnit.SECONDS);
    }
}
