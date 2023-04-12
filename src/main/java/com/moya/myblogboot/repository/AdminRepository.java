package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Admin;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdminRepository implements AdminRepositoryInf{

    private final EntityManager em;

    public AdminRepository(EntityManager em){
        this.em = em;
    }
    @Override
    public Optional<Admin> findById(String id) {
        Admin admin = em.createQuery("select a from Admin a where a.id= :id", Admin.class)
                .setParameter("id", id)
                .getSingleResult();
        return Optional.ofNullable(admin);
    }
}
