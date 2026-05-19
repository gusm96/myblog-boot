package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.configuration.LoginAttemptProperties;
import com.moya.myblogboot.domain.login.LoginAttemptResult;
import com.moya.myblogboot.repository.LoginAttemptRedisRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class LoginAttemptRedisRepositoryImpl implements LoginAttemptRedisRepository {

    private static final String CHECK = "CHECK";
    private static final String FAIL = "FAIL";
    private static final String RESET = "RESET";

    private final StringRedisTemplate redisTemplate;
    private final LoginAttemptProperties properties;
    private final DefaultRedisScript<String> script;

    public LoginAttemptRedisRepositoryImpl(
            @Qualifier("refreshTokenRedisTemplate") StringRedisTemplate redisTemplate,
            LoginAttemptProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/login-attempt.lua"));
        this.script.setResultType(String.class);
    }

    @Override
    public long checkLockTtlMs(String lockKey) {
        String result = redisTemplate.execute(script, List.of(lockKey), CHECK);
        return parseLong(result);
    }

    @Override
    public LoginAttemptResult recordFailure(String failKey, String lockKey) {
        String result = redisTemplate.execute(
                script,
                List.of(failKey, lockKey),
                FAIL,
                String.valueOf(properties.windowMs()),
                lockStagesArg()
        );
        String[] parts = result == null ? new String[0] : result.split(":");
        if (parts.length != 3) {
            throw new IllegalStateException("Unexpected login attempt Redis result: " + result);
        }
        int count = Integer.parseInt(parts[0]);
        long lockTtlMs = Long.parseLong(parts[1]);
        boolean locked = "1".equals(parts[2]) || lockTtlMs > 0;
        return new LoginAttemptResult(count, toRetryAfterSeconds(lockTtlMs), locked);
    }

    @Override
    public void reset(String failKey, String lockKey) {
        redisTemplate.execute(script, List.of(failKey, lockKey), RESET);
    }

    private String lockStagesArg() {
        return properties.lockStages().stream()
                .map(stage -> stage.failureCount() + "=" + stage.lockMs())
                .collect(Collectors.joining(","));
    }

    private long parseLong(String value) {
        return value == null || value.isBlank() ? 0L : Long.parseLong(value);
    }

    private long toRetryAfterSeconds(long ttlMs) {
        if (ttlMs <= 0) {
            return 0;
        }
        return (ttlMs + 999) / 1000;
    }
}
