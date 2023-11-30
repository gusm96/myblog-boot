package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.BoardLikeCountRedisRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BoardLikeCountRedisRepositoryImpl implements BoardLikeCountRedisRepository {

    private static final String BOARD_LIKE_COUNT_KEY = "boardLikeCount:";
    private static final String BOARD_LIKE_COUNT_HASH_KEY = "count";
    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, Object> hashOperations;
    public BoardLikeCountRedisRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
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

    @Override
    public Long incrementBoardLikeCount(Long boardId) {
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        hashOperations.increment(key, BOARD_LIKE_COUNT_HASH_KEY, 1L);
        return ((Number) hashOperations.get(key, BOARD_LIKE_COUNT_HASH_KEY)).longValue();
    }

    @Override
    public Long decrementBoardLikeCount(Long boardId) {
        String key = BOARD_LIKE_COUNT_KEY + boardId;
        hashOperations.increment(key, BOARD_LIKE_COUNT_HASH_KEY, -1L);
        return ((Number) hashOperations.get(key, BOARD_LIKE_COUNT_HASH_KEY)).longValue();
    }
}
