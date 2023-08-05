package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.guest.Guest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GuestRepository implements GuestRepositoryInf {
    private final EntityManager em;

    @Override
    public Long save(Guest guest) {
        em.persist(guest);
        return guest.getId();
    }

    @Override
    public Optional<Guest> findByName(String username) {
        try {
        Guest guest = em.createQuery("select g from Guest g where g.username =: username", Guest.class)
                .setParameter("username", username)
                .getSingleResult();
            return Optional.of(guest);
        }catch (NoResultException e){
            return Optional.empty();
        }
    }
}
