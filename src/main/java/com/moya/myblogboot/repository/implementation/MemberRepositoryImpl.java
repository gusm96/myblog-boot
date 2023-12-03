package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final EntityManager em;

    @Override
    public void save(Member member) {
        em.persist(member);
    }

    @Override
    public Optional<Member>  findByUsername (String username) {
        try {
            Member findMember = em.createQuery("select m from Member m where m.username =: username", Member.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.ofNullable(findMember);
        } catch (NoResultException e) {
            return Optional.empty();
        }

    }

    @Override
    public Optional<Member> findById(Long memberId) {
            Member findMember = em.createQuery("select m from Member m where m.id =: memberId", Member.class)
                    .setParameter("memberId", memberId)
                    .getSingleResult();
            return Optional.ofNullable(findMember);
    }
}
