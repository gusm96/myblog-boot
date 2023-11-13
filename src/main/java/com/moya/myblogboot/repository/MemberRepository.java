package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepository implements MemberRepositoryInf {

    private final EntityManager em;
    @Override
    public Long save(Member member) {
        em.persist(member);
        return member.getId();
    }

    @Override
    public Optional<Member>  findByUsername (String username) {
            Member findMember = em.createQuery("select m from Member m where m.username =: username", Member.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.ofNullable(findMember);
    }

    @Override
    public Optional<Member> findById(Long memberId) {
            Member findMember = em.createQuery("select m from Member m where m.id =: memberId", Member.class)
                    .setParameter("memberId", memberId)
                    .getSingleResult();
            return Optional.ofNullable(findMember);
    }
}
