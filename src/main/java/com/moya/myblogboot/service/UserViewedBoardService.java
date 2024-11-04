package com.moya.myblogboot.service;

public interface UserViewedBoardService {
    boolean isViewedBoard(Long userNum, Long boardId);

    void addViewedBoard(Long userNum, Long boardId);
}
