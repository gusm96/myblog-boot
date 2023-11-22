package com.moya.myblogboot.repository;

public interface RefreshTokenRedisRepositoryInf {

    Long save(Long memberId, String refreshToken);

    String findRefreshTokenById(Long memberId);

    void delete(Long memberId);
}
