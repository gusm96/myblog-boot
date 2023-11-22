package com.moya.myblogboot.repository;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BoardLikeCountRedisRepository implements BoardLikeCountRedisRepositoryInf {

    private static final String BOARD_LIKE_COUNT_KEY = "boardLikeCount:";
    private static final String BOARD_LIKE_COUNT_HASH_KEY = "count";
    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, Object> hashOperations;
    public BoardLikeCountRedisRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(Long boardId) {
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        hashOperations.put(key, BOARD_LIKE_COUNT_HASH_KEY, 0L);
    }

    @Override
    public Long findBoardLikeCount(Long boardId) {
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        return ((Number) hashOperations.get(key, BOARD_LIKE_COUNT_HASH_KEY)).longValue();
    }

    @Override
    public void update(Long boardId, Long count) {
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        hashOperations.put(key, BOARD_LIKE_COUNT_HASH_KEY, count);
    }
}
