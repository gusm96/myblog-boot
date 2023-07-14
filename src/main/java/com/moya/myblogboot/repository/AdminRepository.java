package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.admin.Admin;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminRepository implements AdminRepositoryInf{

    private final EntityManager em;
    @Override
    public Optional<Admin> findById(String adminName) {
        Admin admin = em.createQuery("select a from Admin a where a.admin_name= :name", Admin.class)
                .setParameter("name", adminName)
                .getSingleResult();
        return Optional.ofNullable(admin);
    }

    @Override
    public Long save(Admin admin){
        em.persist(admin);
        return admin.getId();
    }
}

