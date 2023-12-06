package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Member;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberRepositoryTest {
    @Autowired
    private MemberRepository memberRepository;
    @DisplayName("회원 저장")
    @Test
    void 회원_저장 () {
        // given
        Member member = Member.builder()
                .username("testuser123")
                .password("testuser123")
                .nickname("").build();
        // when
        Member savedMember = memberRepository.save(member);
        Member findMember = memberRepository.findById(savedMember.getId()).get();
        Member findMemberByUsername = memberRepository.findByUsername(member.getUsername()).get();
        // then
        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMemberByUsername.getId()).isEqualTo(member.getId());
    }
}