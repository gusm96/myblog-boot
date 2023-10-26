package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.member.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepository implements RoleRepositoryInf {

    private final EntityManager em;
    public int save(Role role) {
        em.persist(role);
        return role.getId();
    }
    @Override
    public Optional<Role> findOne(String roleName) {
        try {
            Role findRole = em.createQuery("select r from Role r where  r.roleName =: roleName", Role.class)
                    .setParameter("roleName", roleName)
                    .getSingleResult();
            return Optional.ofNullable(findRole);
        }catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
