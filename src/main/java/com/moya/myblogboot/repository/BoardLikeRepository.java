package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.BoardLike;
import org.springframework.data.repository.CrudRepository;

public interface BoardLikeRepository extends CrudRepository <BoardLike,String> {

}
