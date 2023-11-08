package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.BoardLikeCount;
import org.springframework.data.repository.CrudRepository;

public interface BoardLikeCountRedisRepository extends CrudRepository<BoardLikeCount, Long> {
}
