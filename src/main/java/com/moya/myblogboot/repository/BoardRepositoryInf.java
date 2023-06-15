package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardResDto;

import java.util.List;
import java.util.Optional;


public interface BoardRepositoryInf {
    // 게시글 작성
    Long upload(Board board);

    // 하나의 게시글 찾기
    Optional<Board> findOne(Long idx);

    // 모든 게시글 찾기
    List<BoardResDto> findAll(int offset, int limit);

    // 해당 type의 게시글 모두 찾기
    List<BoardResDto> findAllBoardsInThatCategory(String categoryName,int offset, int limit);

}
