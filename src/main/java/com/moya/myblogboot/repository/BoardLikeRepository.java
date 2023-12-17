package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
