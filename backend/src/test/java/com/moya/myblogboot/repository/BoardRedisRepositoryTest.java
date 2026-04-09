package com.moya.myblogboot.repository;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.dto.board.BoardForRedis;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class BoardRedisRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    AdminRepository adminRepository;
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static Long boardId;

    @BeforeEach
    void before() {
        Admin admin = Admin.builder()
                .username("testAdmin")
                .password("testPassword")
                .build();
        Admin savedAdmin = adminRepository.save(admin);

        Category newCategory = Category.builder().name("Category").build();
        Category category = categoryRepository.save(newCategory);

        Board newBoard = Board.builder()
                .title("제목")
                .content("내용")
                .category(category)
                .admin(savedAdmin)
                .build();
        Board board = boardRepository.save(newBoard);
        boardId = board.getId();
    }

    @Test
    @DisplayName("게시글 조회 v3")
    void 게시글_조회_V3() {
        Board findBoard = boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("게시글이 존재하지 않습니다.")
        );
        BoardForRedis boardDto = BoardForRedis.builder()
                .board(findBoard)
                .build();

        String key = "board:" + findBoard.getId();
        redisTemplate.opsForValue().set(key, boardDto);
        BoardForRedis redisBoard = (BoardForRedis) redisTemplate.opsForValue().get(key);
        redisBoard.incrementViews();
        redisTemplate.opsForValue().set(key, redisBoard);

        assertThat(redisBoard.getId()).isEqualTo(findBoard.getId());
    }
}
