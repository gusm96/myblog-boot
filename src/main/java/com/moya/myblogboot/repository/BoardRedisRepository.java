package com.moya.myblogboot.repository;

public interface BoardRedisRepository {
    // 좋아요
    void addLike(Long boardId, Long memberId);

    // 좋아요 여부 확인
    boolean isMember(Long boardId, Long memberId);
    // 좋아요 취소
    void likesCancel(Long boardId, Long memberId);

    Long getLikesCount(Long boarId);
    Long getViews(Long boardId);

    Long viewsIncrement(Long boardId);

    void setViews(Long boardId, Long views);

}
