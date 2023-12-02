package com.moya.myblogboot.repository;

public interface BoardLikeCountRedisRepository {

    void save(Long boardId);

    Long findBoardLikeCount(Long boardId);

    Long incrementBoardLikeCount(Long boardId);

    Long decrementBoardLikeCount(Long boardId);

}
