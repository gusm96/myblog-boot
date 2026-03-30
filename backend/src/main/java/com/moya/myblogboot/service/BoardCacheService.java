package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardCacheService {

    private final BoardRedisRepository boardRedisRepository;
    private final BoardRepository boardRepository;

    // Redis 캐시 조회 (miss 시 DB 조회 후 캐시 저장)
    public BoardForRedis getBoardFromCache(Long boardId) {
        return boardRedisRepository.findOne(boardId)
                .orElseGet(() -> retrieveAndCache(boardId));
    }

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

    private BoardForRedis retrieveAndCache(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BOARD_NOT_FOUND));
        return boardRedisRepository.save(board);
    }
}
