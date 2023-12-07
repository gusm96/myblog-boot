package com.moya.myblogboot.repository;

public interface BoardLikeRedisRepository {
    // 좋아요
    void add(Long boardId, Long memberId);
    // 좋아요 수
    Long getCount(Long boardId);
    // 좋아요 여부 확인
    boolean isMember(Long boardId, Long memberId);
    // 좋아요 취소
    void cancel(Long boardId, Long memberId);
}
