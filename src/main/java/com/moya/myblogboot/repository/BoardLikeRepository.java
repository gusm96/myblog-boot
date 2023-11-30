package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.BoardLike;

import java.util.Optional;

public interface BoardLikeRepository {

    void save(BoardLike boardLike);

    Optional<BoardLike> findByBoardId(Long boardId);
}
