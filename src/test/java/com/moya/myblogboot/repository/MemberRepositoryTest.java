package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberRepositoryTest {
    @Autowired
    EntityManager em;
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
        em.persist(member);
        em.flush();
        em.clear();
        Member findMember = em.find(Member.class, member.getId());
        // then
        assertThat(findMember.getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("username으로 회원 찾기")
    void findByUsername() {
        // given
        String username = "testMember";
        // when
        Member findMember = em.createQuery("select m from Member m where m.username = : username", Member.class)
                .setParameter("username", username)
                .getSingleResult();
        // then
        assertThat(username).isEqualTo(findMember.getUsername());
    }
}