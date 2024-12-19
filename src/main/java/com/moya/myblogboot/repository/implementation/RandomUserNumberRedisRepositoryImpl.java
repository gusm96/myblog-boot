package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.RandomUserNumberRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

import static com.moya.myblogboot.domain.keys.RedisKey.RANDOM_USER_NUM_KEY;


@Slf4j
@Repository
@RequiredArgsConstructor
public class RandomUserNumberRedisRepositoryImpl implements RandomUserNumberRedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(long number, long expireTime) {
        redisTemplate.opsForSet().add(RANDOM_USER_NUM_KEY, number);
        redisTemplate.expire(RANDOM_USER_NUM_KEY, expireTime, TimeUnit.SECONDS);
    }

    @Override
    public boolean isExists(long number) {
        return redisTemplate.opsForSet().isMember(RANDOM_USER_NUM_KEY, number);
    }

}
