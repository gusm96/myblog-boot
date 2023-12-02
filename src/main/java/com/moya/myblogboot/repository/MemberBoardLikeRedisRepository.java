package com.moya.myblogboot.repository;

public interface MemberBoardLikeRedisRepository {
    void save(Long memberId, Long boardId);
    boolean isMember(Long memberId, Long boardId);
    void delete(Long memberId, Long boardId);
}
