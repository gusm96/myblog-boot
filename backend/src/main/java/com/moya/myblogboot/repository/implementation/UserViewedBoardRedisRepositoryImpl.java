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
    public void save(Long userNum, Long boardId) {
        String key = getKey(userNum);
        redisTemplate.opsForSet().add(key, boardId);
        redisTemplate.expire(key, TTLCalculator.calculateSecondsUntilMidnight(), TimeUnit.SECONDS);
    }

    @Override
    public boolean isExists(Long userNum, Long boardId) {
        return redisTemplate.opsForSet().isMember(getKey(userNum), boardId);
    }

    private String getKey(Long userNum) {
        return USER_VIEWED_BOARD_KEY + userNum;
    }
}
