package com.moya.myblogboot.repository;

import java.util.List;

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

    Long setViews(Long boardId, Long views);

    void deleteViews(Long id);

    List<Long> getLikesMembers(Long boardId);

    void deleteLikes(Long boardId);

    List<Long> getKeysValues(String key);
}
