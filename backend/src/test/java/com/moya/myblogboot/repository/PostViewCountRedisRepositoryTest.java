package com.moya.myblogboot.repository;


import com.moya.myblogboot.AbstractContainerBaseTest;
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
public class PostViewCountRedisRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Save")
    void save() {
        // given
        // UserNum, PostId, ExpireTime (TTL)
        Long userNum = 30212345L;
        Long postId = 321L;

        String key = "userViewedPosts:" + userNum;
        // when
        redisTemplate.opsForSet().add(key, postId);
        Set<Object> result = redisTemplate.opsForSet().members(key);
        // then
        Assertions.assertTrue(result.contains(postId.intValue()));
    }
}
