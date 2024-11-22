package com.moya.myblogboot.repository;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
public class BoardViewCountRedisRepositoryTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Save")
    void save() {
        // given
        // UserNum, BoardId, ExpireTime (TTL)
        Long userNum = 30212345L;
        Long boardId = 321L;

        String key = "userViewedBoards:" + userNum;
        // when
        redisTemplate.opsForSet().add(key, boardId);
        Set<Object> result = redisTemplate.opsForSet().members(key);
        // then
        Assertions.assertTrue(result.contains(boardId.intValue()));
    }
}
