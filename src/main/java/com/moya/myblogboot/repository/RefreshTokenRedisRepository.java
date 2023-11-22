package com.moya.myblogboot.repository;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.security.cert.Certificate;

@Repository
public class RefreshTokenRedisRepository implements RefreshTokenRedisRepositoryInf {

    private static final String REFRESH_TOKEN_KEY = "refreshToken:";
    private static final String REFRESH_TOKEN_HASH_KEY = "tokenValue";
    private RedisTemplate<String, String> redisTemplate;
    private HashOperations<String, String, String> hashOperations;

    public RefreshTokenRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }
    @Override
    public Long save(Long memberId, String refreshToken) {
        String key = REFRESH_TOKEN_KEY + memberId;
        hashOperations.put(key, REFRESH_TOKEN_HASH_KEY, refreshToken);
        return memberId;
    }

    @Override
    public String findRefreshTokenById(Long memberId) {
        String key = REFRESH_TOKEN_KEY + memberId;
        return hashOperations.get(key, REFRESH_TOKEN_HASH_KEY);
    }

    @Override
    public void delete(Long memberId) {
        String key = REFRESH_TOKEN_KEY + memberId;
        hashOperations.delete(key, REFRESH_TOKEN_HASH_KEY);
    }
}
