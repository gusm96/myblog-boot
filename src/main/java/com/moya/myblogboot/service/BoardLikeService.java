package com.moya.myblogboot.service;

public interface BoardLikeService {

    Long addLikeToBoard(Long memberId, Long boardId);

    boolean isBoardLiked(Long memberId, Long boardId);

    Long deleteBoardLike(Long memberId, Long boardId);

    Long addLikeV2(Long boardId, Long memberId);

    Long cancelLikes(Long boardId, Long memberId);

    boolean isBoardLikedV2(Long boardId, Long memberId);
}
