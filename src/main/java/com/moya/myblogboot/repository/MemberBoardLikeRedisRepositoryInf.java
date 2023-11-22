package com.moya.myblogboot.repository;


public interface MemberBoardLikeRedisRepositoryInf {

    void save(Long memberId, Long boardId);

    boolean isMember(Long memberId, Long boardId);

    void delete(Long memberId, Long boardId);

}
