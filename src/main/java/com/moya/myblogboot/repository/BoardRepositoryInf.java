package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardReq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface BoardRepositoryInf {
    // 게시글 작성
    Long upload(Board board);

    // 하나의 게시글 찾기
    Optional<Board> findOne(long idx);

    // 모든 게시글 찾기
    List<Board> findAllPosts(int offset, int limit);

    // 해당 type의 게시글 모두 찾기
    List<Board> findAllPostsOfThatType(int board_type,int offset, int limit);

}
