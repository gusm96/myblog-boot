package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.token.RefreshToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TokenRepository implements TokenRepositoryInf {

    private final EntityManager em;
    @Override
    public void save(RefreshToken token) {
        em.persist(token);
    }

    @Override
    public Optional<RefreshToken> findOne(String username) {
        try{
            RefreshToken token = em.createQuery(
                            "select t from RefreshToken t where t.username =: username",
                            RefreshToken.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.of(token);
        }catch (NoResultException e){
            return Optional.empty();
        }
    }
    @Override
    public void delete(RefreshToken token) {
        em.remove(token);
    }
}
