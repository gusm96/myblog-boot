package com.moya.myblogboot.service;

import com.moya.myblogboot.repository.BoardLikeRedisRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class BoardLikeServiceTest {

    private static final int COUNT = 10000;
    private static final ExecutorService service = Executors.newFixedThreadPool(COUNT);
    @Autowired
    private BoardLikeRedisRepository boardLikeRedisRepository;


    @DisplayName("Redis 게시글 좋아요 테스트")
    @Test
    void boardLike() throws InterruptedException {
        // given
        // 게시글 ID
        Long boardId = 1L;
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            Long memberId = (long) i;
            service.execute(()-> {
                boardLikeRedisRepository.save(boardId, memberId);
                latch.countDown();
            });
        }
        // then
        latch.await();
        Assertions.assertEquals(COUNT, boardLikeRedisRepository.getCount(boardId));
    }

    @DisplayName("Redis 게시글 좋아요 취소 테스트")
    @Test
    void boardLikeCancel() throws InterruptedException {
        // given
        // 게시글 ID
        Long boardId = 1L;
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            Long memberId = (long) i;
            service.execute(()-> {
                boardLikeRedisRepository.delete(boardId, memberId);
                latch.countDown();
            });
        }
        // then
        latch.await();
        Assertions.assertEquals(0L, boardLikeRedisRepository.getCount(boardId));
    }

}
