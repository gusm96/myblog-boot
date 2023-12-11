package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @DisplayName("게시글 조회수 증가")
    @Test
    void 게시글_조회수 () throws InterruptedException {
        // given
        Long boardId =  1L;
        String key = "views:" + boardId;
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            service.execute(()->{
                redisTemplate.opsForValue().increment(key); // INCR 명령을 사용하여 값을 원자적으로 증가
                latch.countDown();
            });
        }
        // then
        latch.await();
        Long views = Long.parseLong(redisTemplate.opsForValue().get(key).toString());
        Assertions.assertThat(10000L).isEqualTo(views);
    }

    @DisplayName("게시글 조회수 증가 Version2")
    @Test
    void 게시글_조회수_V2() {
        // given
        Long boardId = 1L;
        Board board = boardRepository.findById(boardId).get();
        Long views = boardRedisRepository.viewsIncrement(boardId);
        if(views == null || views < board.getViews()){
            boardRedisRepository.setViews(board.getId(), board.getViews() + 1);
        }


        // then
    }
}
