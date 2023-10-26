package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.BoardLike;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardLikeService {
    private final RedisTemplate<String, BoardLike> redisTemplate;

    public void addLike(Long boardId, Long guestId) {
        // Key (String)
        String key = "board_like: " + boardId + guestId;
        // Value (Object)
        BoardLike boardLike = new BoardLike(key, boardId, guestId);
        // Save
        redisTemplate.opsForSet().add(key, boardLike);
    }
    public void cancelLike(Long boardId, Long guestId){

    }
}
