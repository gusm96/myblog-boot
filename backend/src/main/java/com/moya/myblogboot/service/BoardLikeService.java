package com.moya.myblogboot.service;

public interface BoardLikeService {

    Long addLikes(Long boardId);

    Long cancelLikes(Long boardId);
}
