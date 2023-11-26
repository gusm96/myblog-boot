package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Member;

import java.util.Optional;

public interface MemberRepository {
    Long save(Member member);
    Optional<Member> findByUsername(String username);

    Optional<Member> findById(Long memberId);
}
