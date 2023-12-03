package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface BoardRepository {
    // 게시글 작성
    Long upload(Board board);

    // 하나의 게시글 찾기
    Optional<Board> findById(Long idx);

    // 모든 게시글 찾기
    List<Board> findAll(int offset, int limit);
    // 게시글 검색
    List<Board> findBySearch(SearchType type, String searchContents, int offset, int limit);

    // 카테고리별 모든 게시글 찾기
    List<Board> findByCategory(String categoryName,int offset, int limit);

    // 게시글 삭제
    void removeBoard(Board board);

    // 조건별 게시글 개수
    Long findAllCount();

    Long findBySearchCount(SearchType type, String searchContents);

    Long findByCategoryCount(String categoryName);

    Optional<Board> findByIdVersion2(Long boardId);
}
