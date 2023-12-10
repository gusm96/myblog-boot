package com.moya.myblogboot.repository;


import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import static com.moya.myblogboot.domain.member.QMember.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslTest {
   /* @Autowired
    private EntityManager em;

    private JPAQueryFactory queryFactory;

    @Before
    void before(){
        Member member1 = Member.builder().username("member1").password("test1234").nickname("member1").build();
        Member member2 = Member.builder().username("member2").password("test1234").nickname("member2").build();
        Member member3 = Member.builder().username("member3").password("test1234").nickname("member3").build();

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
    }
    @Test
    void jpql () {
        // given
        Long memberId = 1L;
        String query = "select m from Member m where m.id = :memberId";
        // when
        Member member = em.createQuery(query, Member.class).setParameter("memberId", memberId).getSingleResult();
        // then
        assertThat(member.getId()).isEqualTo(memberId);
    }

    @DisplayName("Querydsl 조회")
    @Test
    void querydsl () {
        // given
        queryFactory = new JPAQueryFactory(em);
        // when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }*/
}
