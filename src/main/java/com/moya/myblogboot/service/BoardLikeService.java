package com.moya.myblogboot.service;

public interface BoardLikeService {

    Long addLike(Long boardId, Long memberId);

    Long cancelLikes(Long boardId, Long memberId);

    boolean isBoardLiked(Long boardId, Long memberId);
}
