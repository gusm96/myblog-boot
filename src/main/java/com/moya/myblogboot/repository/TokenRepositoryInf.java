package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.token.RefreshToken;
import com.moya.myblogboot.domain.token.TokenUserType;

import java.util.Optional;

public interface TokenRepositoryInf {
    void save(RefreshToken token);

    Optional<RefreshToken> findOne(String username, TokenUserType tokenUserType);

    void delete(RefreshToken token);
}
