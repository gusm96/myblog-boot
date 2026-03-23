package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.repository.VisitorCountRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.moya.myblogboot.domain.keys.RedisKey.*;


@Slf4j
@Repository
@RequiredArgsConstructor
public class VisitorCountRedisRepositoryImpl implements VisitorCountRedisRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String keyDate, VisitorCountDto visitorCountDto) {
        String key = getKey(keyDate);
        redisTemplate.opsForHash().putAll(key, visitorCountDto.toMap());
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    @Override
    public Optional<VisitorCountDto> increment(String keyDate) {
        String key = getKey(keyDate);

        // Cold Cache 감지: 키가 없으면 서비스 레이어의 DB 복구 블록으로 위임
        // hasKey()는 Boolean(nullable)이므로 Boolean.FALSE.equals()로 null-safe 처리
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            return Optional.empty();
        }

        redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
        redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
        return findByDate(keyDate);
    }

    @Override
    public Optional<VisitorCountDto> findByDate(String keyDate) {
        String key = getKey(keyDate);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(VisitorCountDto.builder()
                .total(getLongValue(entries.get(TOTAL_COUNT_KEY)))
                .today(getLongValue(entries.get(TODAY_COUNT_KEY)))
                .yesterday(getLongValue(entries.get(YESTERDAY_COUNT_KEY)))
                .build());
    }

    private Long getLongValue(Object value) {
        return (value instanceof Number) ? ((Number) value).longValue() : 0L;
    }

    private String getKey(String keyDate) {
        return VISITOR_COUNT_KEY + keyDate;
    }
}
