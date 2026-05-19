package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.configuration.LoginAttemptProperties;
import com.moya.myblogboot.domain.login.LoginAttemptResult;
import com.moya.myblogboot.exception.custom.TooManyLoginAttemptsException;
import com.moya.myblogboot.repository.LoginAttemptRedisRepository;
import com.moya.myblogboot.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static com.moya.myblogboot.domain.keys.RedisKey.LOGIN_FAIL_ACCOUNT_KEY;
import static com.moya.myblogboot.domain.keys.RedisKey.LOGIN_FAIL_IP_KEY;
import static com.moya.myblogboot.domain.keys.RedisKey.LOGIN_LOCK_ACCOUNT_KEY;
import static com.moya.myblogboot.domain.keys.RedisKey.LOGIN_LOCK_IP_KEY;

@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final LoginAttemptRedisRepository loginAttemptRedisRepository;
    private final LoginAttemptProperties properties;

    @Override
    public void assertNotLocked(String username, String clientIp) {
        LoginKeys keys = loginKeys(username, clientIp);
        long accountTtlMs = loginAttemptRedisRepository.checkLockTtlMs(keys.accountLockKey());
        long ipTtlMs = loginAttemptRedisRepository.checkLockTtlMs(keys.ipLockKey());
        long retryAfterMs = Math.max(accountTtlMs, ipTtlMs);
        if (retryAfterMs > 0) {
            throw new TooManyLoginAttemptsException(toRetryAfterSeconds(retryAfterMs));
        }
    }

    @Override
    public LoginAttemptResult onFailure(String username, String clientIp) {
        LoginKeys keys = loginKeys(username, clientIp);
        LoginAttemptResult accountResult = loginAttemptRedisRepository.recordFailure(
                keys.accountFailKey(), keys.accountLockKey());
        LoginAttemptResult ipResult = loginAttemptRedisRepository.recordFailure(
                keys.ipFailKey(), keys.ipLockKey());

        int count = Math.max(accountResult.count(), ipResult.count());
        long retryAfterSeconds = Math.max(accountResult.retryAfterSeconds(), ipResult.retryAfterSeconds());
        boolean locked = accountResult.locked() || ipResult.locked();
        return new LoginAttemptResult(count, retryAfterSeconds, locked);
    }

    @Override
    public void onSuccess(String username, String clientIp) {
        LoginKeys keys = loginKeys(username, clientIp);
        loginAttemptRedisRepository.reset(keys.accountFailKey(), keys.accountLockKey());
        loginAttemptRedisRepository.reset(keys.ipFailKey(), keys.ipLockKey());
    }

    @Override
    public void applyProgressiveDelay(int count) {
        long delayMs = Math.min(
                multiplySafely(properties.baseDelayMs(), count),
                properties.maxDelayMs()
        );
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private LoginKeys loginKeys(String username, String clientIp) {
        String accountHash = sha256Hex(username);
        String ipHash = sha256Hex(clientIp);
        return new LoginKeys(
                String.format(LOGIN_FAIL_ACCOUNT_KEY, accountHash),
                String.format(LOGIN_LOCK_ACCOUNT_KEY, accountHash),
                String.format(LOGIN_FAIL_IP_KEY, ipHash),
                String.format(LOGIN_LOCK_IP_KEY, ipHash)
        );
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private long multiplySafely(long baseDelayMs, int count) {
        if (count <= 1) {
            return baseDelayMs;
        }
        long result = baseDelayMs;
        for (int i = 1; i < count; i++) {
            if (result >= properties.maxDelayMs()) {
                return properties.maxDelayMs();
            }
            result *= 2;
        }
        return result;
    }

    private long toRetryAfterSeconds(long retryAfterMs) {
        return (retryAfterMs + 999) / 1000;
    }

    private record LoginKeys(
            String accountFailKey,
            String accountLockKey,
            String ipFailKey,
            String ipLockKey
    ) {
    }
}
