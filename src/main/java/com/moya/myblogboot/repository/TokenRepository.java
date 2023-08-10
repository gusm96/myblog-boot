package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.token.RefreshToken;
import com.moya.myblogboot.domain.token.TokenUserType;
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
    public Optional<RefreshToken> findOne(String refresh_token) {
        try{
            RefreshToken token = em.createQuery(
                            "select t from RefreshToken t where t.token =: refresh_token",
                            RefreshToken.class)
                    .setParameter("refresh_token", refresh_token)
                    .getSingleResult();
            return Optional.of(token);
        }catch (NoResultException e){
            return Optional.empty();
        }
    }

    @Override
    public Optional<RefreshToken> findByNmaeAndUserType(String username, TokenUserType tokenUserType) {
        try{
            RefreshToken token = em.createQuery("select t from RefreshToken t " +
                            "where t.username =: username and t.tokenUserType =: tokenUserType", RefreshToken.class)
                    .setParameter("usersname", username)
                    .setParameter("tokenUserType", tokenUserType)
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
