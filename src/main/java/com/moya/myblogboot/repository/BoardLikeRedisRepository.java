package com.moya.myblogboot.repository;

public interface BoardLikeRedisRepository {
    void save(Long boardId, Long memberId);
    Long getCount(Long boardId);
    boolean isMember(Long boardId, Long memberId);
    void delete(Long boardId, Long memberId);
}
