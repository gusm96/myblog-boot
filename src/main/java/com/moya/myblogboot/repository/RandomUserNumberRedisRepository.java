package com.moya.myblogboot.repository;

public interface RandomUserNumberRedisRepository {

    boolean isExists(long number);
    void save(long number, long expireTime);
}
