package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.RandomUserNumberRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static com.moya.myblogboot.domain.keys.RedisKey.RANDOM_USER_NUM_KEY;


@Repository
@RequiredArgsConstructor
public class RandomUserNumberRedisRepositoryImpl implements RandomUserNumberRedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");

    @Override
    public void save(long number, long expireTime) {
        String key = RANDOM_USER_NUM_KEY + number;
        String formattedNow = getCurrentFormattedTime();
        redisTemplate.opsForValue().set(key, formattedNow, expireTime, TimeUnit.SECONDS);
    }

    @Override
    public boolean isExists(long number) {
        return redisTemplate.opsForValue().get(String.valueOf(number)) != null;
    }

    private String getCurrentFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(FORMATTER);
    }


}
