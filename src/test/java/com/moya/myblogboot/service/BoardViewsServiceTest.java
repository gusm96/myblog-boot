package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Transactional
public class BoardViewsServiceTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private BoardRedisRepository boardRedisRepository;
    private static final int COUNT = 10000;
    private static final ExecutorService service = Executors.newFixedThreadPool(COUNT);

    private static final String KEY = "testViews:1";
    @BeforeEach
    void setUp() {
        // 초기 데이터를 Redis에 추가하는 작업 수행
        redisTemplate.opsForValue().set(KEY, 0L);
    }
    @AfterEach
    void tearDown() {
        // 테스트가 끝난 후에는 Redis에서 데이터를 삭제하거나 초기 상태로 되돌리는 작업 수행
        redisTemplate.delete(KEY);
    }
    @DisplayName("게시글 조회수 증가")
    @Test
    void 게시글_조회수 () throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            service.execute(()->{
                redisTemplate.opsForValue().increment(KEY); // INCR 명령을 사용하여 값을 원자적으로 증가
                latch.countDown();
            });
        }
        // then
        latch.await();
        Long views = Long.parseLong(redisTemplate.opsForValue().get(KEY).toString());
        Assertions.assertThat(10000L).isEqualTo(views);
    }
}


