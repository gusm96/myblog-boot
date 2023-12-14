package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Member;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void before(){
        Member newMember = Member.builder()
                .username("testMember")
                .password("testPassword")
                .nickname("testMember")
                .build();
        em.persist(newMember);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("회원 저장")
    void 회원_저장 () {
        // given
        Member member = Member.builder()
                .username("testuser123")
                .password("testuser123")
                .nickname("").build();
        // when
        Member savedMember = memberRepository.save(member);
        Member findMember = memberRepository.findById(savedMember.getId()).get();
        // then
        assertThat(findMember.getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("username으로 회원 찾기")
    void findByUsername() {
        // given
        String username = "testMember";
        // when
        Member findMember = memberRepository.findByUsername(username).get();
        // then
        assertThat(username).isEqualTo(findMember.getUsername());
    }

    @Test
    @DisplayName("username으로 회원 존재 여부 테스트")
    void isExistsByUsername() {
        // given
        String username1 = "testMember";
        String username2 = "testMember2";
        // when
        boolean result1 = memberRepository.existsByUsername(username1);
        boolean result2 = memberRepository.existsByUsername(username2);
        // then
        assertTrue(result1);
        assertFalse(result2);
    }
}