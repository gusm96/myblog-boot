package com.moya.myblogboot.service;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.board.BoardLike;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class BoardLikeServiceTest extends AbstractContainerBaseTest {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @DisplayName("게시글 좋아요")
    @Test
    void 게시글_좋아요 () {
        // given
        Long boardId = 1L;
        Long guestId = 1L;
        String key = "board_like: "+ boardId;
        BoardLike boardLike = new BoardLike(key, boardId, guestId);
        // when
        redisTemplate.opsForValue().set(key, boardLike);
        Long count = redisTemplate.opsForSet().size(key);

        // then
        Assertions.assertThat(count).isEqualTo("1");
    }
}

