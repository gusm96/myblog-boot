package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.UserBoardLike;
import org.springframework.data.repository.CrudRepository;

public interface BoardLikeRepository extends CrudRepository <UserBoardLike,String> {

}
