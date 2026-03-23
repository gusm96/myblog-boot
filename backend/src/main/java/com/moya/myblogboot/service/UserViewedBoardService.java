package com.moya.myblogboot.service;

public interface UserViewedBoardService {
    boolean isViewedBoard(String userToken, Long boardId);

    void addViewedBoard(String userToken, Long boardId);
}
