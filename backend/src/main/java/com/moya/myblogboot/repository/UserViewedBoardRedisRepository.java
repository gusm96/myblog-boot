package com.moya.myblogboot.repository;

public interface UserViewedBoardRedisRepository {
    void save(String userToken, Long boardId);

    boolean isExists(String userToken, Long boardId);
}
