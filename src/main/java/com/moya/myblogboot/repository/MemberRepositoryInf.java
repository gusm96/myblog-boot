package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Member;

import java.util.Optional;

public interface MemberRepositoryInf {
    Long save(Member member);
    Optional<Member> findOne(String username);
}
