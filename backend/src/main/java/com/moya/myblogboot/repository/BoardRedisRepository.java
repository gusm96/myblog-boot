package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;

import java.util.Optional;
import java.util.Set;

public interface BoardRedisRepository {

    Set<Long> getKeys(String pattern);

    Optional<BoardForRedis> findOne(Long boardId);

    BoardForRedis incrementViews(BoardForRedis boardForRedis);

    BoardForRedis incrementLikes(BoardForRedis board);
    
    BoardForRedis decrementLikes(BoardForRedis board);

    BoardForRedis save(Board board);

    void delete(BoardForRedis board);

    void update(BoardForRedis board);

    boolean isDuplicateBoardViewCount(String key);

    void saveClientIp(String key);
}
