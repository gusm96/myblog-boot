package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.MemberBoardLike;
import org.springframework.data.repository.CrudRepository;

public interface MemberBoardLikeRedisRepository extends CrudRepository <MemberBoardLike,Long> {
}
