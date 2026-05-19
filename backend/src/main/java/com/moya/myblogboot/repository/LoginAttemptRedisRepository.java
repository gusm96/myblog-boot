package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.login.LoginAttemptResult;

public interface LoginAttemptRedisRepository {

    long checkLockTtlMs(String lockKey);

    LoginAttemptResult recordFailure(String failKey, String lockKey);

    void reset(String failKey, String lockKey);
}
