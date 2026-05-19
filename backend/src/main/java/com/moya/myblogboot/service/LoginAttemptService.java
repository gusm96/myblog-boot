package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.login.LoginAttemptResult;

public interface LoginAttemptService {

    void assertNotLocked(String username, String clientIp);

    LoginAttemptResult onFailure(String username, String clientIp);

    void onSuccess(String username, String clientIp);

    void applyProgressiveDelay(int count);
}
