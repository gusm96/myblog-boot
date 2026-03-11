package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardCacheService {

    private final BoardRedisRepository boardRedisRepository;

    @Async
    public void updateBoard(BoardForRedis boardForRedis, Board board) {
        boardForRedis.update(board);
        try {
            boardRedisRepository.update(boardForRedis);
        } catch (Exception e) {
            log.error("Redis 캐시 업데이트 실패 (boardId={}): {}", boardForRedis.getId(), e.getMessage());
        }
    }

    public void deleteBoard(BoardForRedis boardForRedis) {
        try {
            boardRedisRepository.delete(boardForRedis);
        } catch (Exception e) {
            log.error("Redis 캐시 삭제 실패 (boardId={}): {}", boardForRedis.getId(), e.getMessage());
        }
    }
}
