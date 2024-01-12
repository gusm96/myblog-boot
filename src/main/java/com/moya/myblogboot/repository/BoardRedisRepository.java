package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;

import java.util.Optional;
import java.util.Set;

public interface BoardRedisRepository {

    Set<Long> getKeysValues(String key);

    Optional<BoardForRedis> findOne(Long boardId);

    BoardForRedis incrementViews(BoardForRedis boardForRedis);

    BoardForRedis save(Board board);

    void delete(BoardForRedis board);

    boolean existsMember(Long boardId, Long memberId);

    void update(BoardForRedis board);
}
