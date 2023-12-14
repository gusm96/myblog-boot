package com.moya.myblogboot.service;

import com.moya.myblogboot.repository.BoardRedisRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BoardLikeServiceTest {
    private static final String KEY = "testLikes:1";
   private static final int COUNT = 10000;
    private static final ExecutorService service = Executors
            .newFixedThreadPool(COUNT);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @AfterEach
    void after(){
        redisTemplate.delete(KEY);
    }

    @Test
    @DisplayName("Redis 게시글 좋아요 테스트")
    void boardLike() throws InterruptedException {
        // given
        // 게시글 ID
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            Long memberId = (long) i;
            service.execute(()-> {
                redisTemplate.opsForSet().add(KEY, memberId);
                latch.countDown();
            });
        }
        // then
        latch.await();
        assertEquals(COUNT, redisTemplate.opsForSet().size(KEY));
    }

    @Test
    @DisplayName("Redis 게시글 좋아요 취소 테스트")
    void boardLikeCancel() throws InterruptedException {
        // given
        // 게시글 ID
        CountDownLatch latch = new CountDownLatch(COUNT);
        // 좋아요 미리 추가.
        for (int i = 0; i < COUNT; i++) {
            Long memberId = (long) i;
            redisTemplate.opsForSet().add(KEY, memberId);
        }
        // when
        for (int i = 0; i < COUNT; i++) {
            Long memberId = (long) i;
            service.execute(()-> {
                redisTemplate.opsForSet().remove(KEY, memberId);
                latch.countDown();
            });
        }
        // then
        latch.await();
        assertEquals(0L, redisTemplate.opsForSet().size(KEY));
    }

    @Test
    @DisplayName("Redis 게시글 좋아요 여부 테스트")
    void isLiked() {
        // given
        Long memberId = 1L;
        // 한건 저장
        redisTemplate.opsForSet().add(KEY, memberId);
        // when
        boolean result = redisTemplate.opsForSet().isMember(KEY, memberId);
        // then
        assertTrue(result);
    }
}
