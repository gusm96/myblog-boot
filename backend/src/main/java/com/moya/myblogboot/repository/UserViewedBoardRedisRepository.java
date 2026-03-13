package com.moya.myblogboot.repository;

public interface UserViewedBoardRedisRepository {
    void save(Long userNum, Long boardId);

    boolean isExists(Long userNum, Long boardId);
}
