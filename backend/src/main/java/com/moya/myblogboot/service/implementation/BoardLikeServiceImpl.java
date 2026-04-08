package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.service.BoardCacheService;
import com.moya.myblogboot.service.BoardLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardLikeServiceImpl implements BoardLikeService {

    private final BoardCacheService boardCacheService;
    private final BoardRedisRepository boardRedisRepository;

    @Override
    public Long addLikes(Long boardId) {
        BoardForRedis board = boardCacheService.getBoardFromCache(boardId);
        return boardRedisRepository.incrementLikes(board).totalLikes();
    }

    @Override
    public Long cancelLikes(Long boardId) {
        BoardForRedis board = boardCacheService.getBoardFromCache(boardId);
        return boardRedisRepository.decrementLikes(board).totalLikes();
    }
}
