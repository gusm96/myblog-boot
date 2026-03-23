package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.UserViewedBoardRedisRepository;
import com.moya.myblogboot.utils.TTLCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

import static com.moya.myblogboot.domain.keys.RedisKey.USER_VIEWED_BOARD_KEY;

@Repository
@RequiredArgsConstructor
public class UserViewedBoardRedisRepositoryImpl implements UserViewedBoardRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String userToken, Long boardId) {
        String key = getKey(userToken);
        redisTemplate.opsForSet().add(key, boardId);
        redisTemplate.expire(key, TTLCalculator.calculateSecondsUntilMidnight(), TimeUnit.SECONDS);
    }

    @Override
    public boolean isExists(String userToken, Long boardId) {
        return redisTemplate.opsForSet().isMember(getKey(userToken), boardId);
    }

    private String getKey(String userToken) {
        return USER_VIEWED_BOARD_KEY + userToken;
    }
}
