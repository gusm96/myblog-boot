package com.moya.myblogboot;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

public final class RedisTestCleaner {

    private RedisTestCleaner() {
    }

    public static void deleteLoginAttemptKeys(StringRedisTemplate redisTemplate) {
        ScanOptions options = ScanOptions.scanOptions()
                .match("login:*")
                .count(100)
                .build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
