package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);

    @Query("select bl from BoardLike bl where bl.board.id = :boardId and bl.member.id = :memberId")
    Optional<BoardLike> findByBoardIdAndMemberId(Long boardId, Long memberId);

}
