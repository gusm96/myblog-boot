package com.moya.myblogboot.repository;

public interface BoardLikeCountRedisRepository {

    void save(Long boardId);

    Long findBoardLikeCount(Long boardId);

    void update(Long boardId, Long count);

    Long incrementBoardLikeCount(Long boardId);

    Long decrementBoardLikeCount(Long boardId);
}
