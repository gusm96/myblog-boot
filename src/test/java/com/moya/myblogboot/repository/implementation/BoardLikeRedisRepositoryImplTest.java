package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.BoardLikeRedisRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BoardLikeRedisRepositoryImplTest {
    @Autowired
    private BoardLikeRedisRepository boardLikeRedisRepository;
    @Test
    void isMember() {
    }

    @Test
    void delete() {
    }

   /* @Test
    void getCount() {
        Long boardId = 1L;

        Long result = boardLikeRedisRepository.getCount(boardId);

        Assertions.assertThat(0L).isEqualTo(result);
    }*/
}