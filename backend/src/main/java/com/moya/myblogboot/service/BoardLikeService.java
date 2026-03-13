package com.moya.myblogboot.service;

public interface BoardLikeService {

    Long addLikes(Long boardId, Long memberId);

    Long cancelLikes(Long boardId, Long memberId);

    boolean isLiked(Long boardId, Long memberId);
}
