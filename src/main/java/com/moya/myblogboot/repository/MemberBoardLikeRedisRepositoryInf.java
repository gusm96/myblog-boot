package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.MemberBoardLike;
import org.springframework.data.repository.CrudRepository;

public interface MemberBoardLikeRedisRepositoryInf extends CrudRepository<MemberBoardLike, Long> {
}
