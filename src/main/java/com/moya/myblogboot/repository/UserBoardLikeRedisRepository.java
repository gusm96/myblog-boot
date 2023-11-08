package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.UserBoardLike;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserBoardLikeRedisRepository extends CrudRepository<UserBoardLike, Long> {

    Optional<UserBoardLike> findByIdAndBoardId(Long memberId, Long boardId);

}
