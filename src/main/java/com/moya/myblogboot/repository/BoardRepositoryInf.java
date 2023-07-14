package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.SearchType;

import java.util.List;
import java.util.Optional;


public interface BoardRepositoryInf {
    // 게시글 작성
    Long upload(Board board);

    // 하나의 게시글 찾기
    Optional<Board> findOne(Long idx);

    // 모든 게시글 찾기
    List<Board> findAll(int offset, int limit);

    // 게시글 검색
    List<Board> findBySearch(SearchType type, String searchContents, int offset, int limit);

    // 카테고리별 모든 게시글 찾기
    List<Board> findAllBoardsInThatCategory(String categoryName,int offset, int limit);


    void removeBoard(Board board);
}
