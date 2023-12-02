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
        hashOperations.put(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY, 0L);
    }

    @Override
    public Long findBoardLikeCount(Long boardId) {
        return getCount(boardId);
    }

    @Override
    public Long incrementBoardLikeCount(Long boardId) {
        hashOperations.increment(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY, 1L);
        return getCount(boardId);
    }

    @Override
    public Long decrementBoardLikeCount(Long boardId) {
        hashOperations.increment(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY, -1L);
        return getCount(boardId);
    }

    private long getCount(Long boardId) {
        return ((Number) hashOperations.get(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY)).longValue();
    }
}
