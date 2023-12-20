package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    Set<Long> getKeysValues(String key);

    Optional<BoardForRedis> findById(Long boardId);

    Optional<BoardForRedis> findOne(Long boardId);
    BoardForRedis save(Board board);

    void delete(Long boardId);

    boolean existsMember(Long boardId, Long memberId);

    Long addLikeV2(Long boardId, Long memberId);

    Long deleteMembers(Long boardId, Long memberId);
}
