package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoardRedisRepositoryImpl implements BoardRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BOARD_LIKE_KEY = "likes:";
    private static final String BOARD_VIEWS_KEY = "views:";
    @Override
    public void add(Long boardId, Long memberId) {
        String key = BOARD_LIKE_KEY + boardId;
        redisTemplate.opsForSet().add(key, memberId);
    }

    @Override
    public boolean isMember(Long boardId, Long memberId) {
        String key = BOARD_LIKE_KEY + boardId;
        return redisTemplate.opsForSet().isMember(key, memberId);
    }

    @Override
    public void cancel(Long boardId, Long memberId) {
        String key = BOARD_LIKE_KEY + boardId;
        redisTemplate.opsForSet().remove(key, memberId);
    }

    @Override
    public Long getCount(Long boardId) {
        String key = BOARD_LIKE_KEY + boardId;

        return (long) redisTemplate.opsForSet().members(key).size();
    }
    @Override
    public Long getViews(Long boardId) {
        String key = BOARD_VIEWS_KEY + boardId;
        return Long.parseLong(redisTemplate.opsForValue().get(key).toString());
    }

    @Override
    public Long viewsIncrement(Long boardId) {
        String key = BOARD_VIEWS_KEY + boardId;
        return redisTemplate.opsForValue().increment(key);
    }
}