package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.MemberRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class BoardLikeServiceTets {

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
}
