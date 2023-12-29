package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BoardRedisRepository {

    Set<Long> getKeysValues(String key);

    Optional<BoardForRedis> findOne(Long boardId);

    BoardForRedis incrementViews(BoardForRedis boardForRedis);

    BoardForRedis save(Board board);

    void delete(Long boardId);

    boolean existsMember(Long boardId, Long memberId);

    Long addLike(Long boardId, Long memberId);

    Long deleteMembers(Long boardId, Long memberId);

    void update(Board board);
}
