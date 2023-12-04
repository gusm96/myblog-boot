package com.moya.myblogboot.service;

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

    private static final int COUNT = 100;
    private static final ExecutorService service = Executors.newFixedThreadPool(COUNT);

    @Autowired
    private AuthService authService;

    @Autowired
    private BoardService boardService;

    /*@Test
    @DisplayName("JPA 좋아요 테스트")
    void incrementBoarLikeCount () {
        // given
        int memberIdx = 2;
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            Long finalMemberIdx = (long) memberIdx;
            service.execute(() -> {
                Member member = authService.retrieveMemberById(finalMemberIdx);
                boardService.addBoardLikeVersion2(20L, member);
                latch.countDown();
            });
            memberIdx++;
        }
        // then
        latch.await();
        Assertions.assertEquals(before + COUNT, numberService.getNumber());
    }*/


    /*@Test
    @DisplayName("카운트 테스트")
    void incrementCount () throws InterruptedException {
        // given
        AtomicInteger count = new AtomicInteger();
        int maxCount = 100;
        CountDownLatch latch = new CountDownLatch(COUNT);
        // when
        for (int i = 0; i < COUNT; i++) {
            service.execute(() -> {
                count.getAndIncrement();
                latch.countDown();
            });
        }
        // then
        latch.await();
        Assertions.assertEquals(maxCount, count);
    }*/
}
