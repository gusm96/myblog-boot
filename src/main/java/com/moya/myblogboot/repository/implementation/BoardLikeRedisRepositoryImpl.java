package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.BoardLikeRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoardLikeRedisRepositoryImpl implements BoardLikeRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY = "boardLike:";
    @Override
    public void add(Long boardId, Long memberId) {
        redisTemplate.opsForSet().add(KEY + boardId, memberId);
    }

    @Override
    public boolean isMember(Long boardId, Long memberId) {
        return redisTemplate.opsForSet().isMember(KEY + boardId, memberId);
    }

    @Override
    public void cancel(Long boardId, Long memberId) {
        redisTemplate.opsForSet().remove(KEY + boardId, memberId);
    }

    @Override
    public Long getCount(Long boardId) {
        return (long) redisTemplate.opsForSet().members(KEY + boardId).size();
    }
}