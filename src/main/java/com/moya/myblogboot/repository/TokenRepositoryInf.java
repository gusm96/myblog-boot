package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.token.RefreshToken;

import java.util.Optional;

public interface TokenRepositoryInf {
    Long save(RefreshToken token);

    Optional<RefreshToken> findRefreshTokenByIndex(Long refresh_token_idx);
    Optional<RefreshToken> findRefreshTokenByUsername(String username);
    void delete(RefreshToken token);
}
