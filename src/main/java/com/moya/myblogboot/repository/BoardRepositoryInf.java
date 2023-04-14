package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Board;

import java.util.List;
import java.util.Optional;

public interface BoardRepositoryInf {
    // 게시글 작성
    Long upload(Board board);

    // 하나의 게시글
    Optional<Board> findOne(long idx);

    // 선택한 게시판의 모든 게시글
    List<Board> findAll(int board_type);
}
