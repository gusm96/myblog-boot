package com.moya.myblogboot.repository;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;


import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BoardRedisRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static Long memberId;
    private static Long categoryId;
    private static Long boardId;

    @BeforeEach
    void before(){
        Member newMember = Member.builder()
                .username("testMember")
                .password("testPassword")
                .nickname("testMember")
                .build();
        Member member = memberRepository.save(newMember);
        memberId = member.getId();

        Category newCategory = Category.builder().name("Category").build();
        Category category = categoryRepository.save(newCategory);
        categoryId = category.getId();

        Board newBoard = Board.builder()
                .title("제목")
                .content("내용")
                .category(category)
                .member(member)
                .build();
        Board board = boardRepository.save(newBoard);
        boardId = board.getId();
    }

    @Test
    @DisplayName("게시글 조회 v3")
    void 게시글_조회_V3 () {
        // given
        Board findBoard = boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("게시글이 존재하지 않습니다.")
        );
        Set<Long> memberIds = findBoard.getBoardLikes().stream().map(boardLike -> boardLike.getMember().getId()).collect(Collectors.toSet());
        BoardForRedis boardDto = BoardForRedis.builder()
                .board(findBoard)
                .memberIds(memberIds)
                .build();

        String key = "board:" + findBoard.getId();
        // when
        redisTemplate.opsForValue().set(key, boardDto);
        BoardForRedis redisBoard = (BoardForRedis) redisTemplate.opsForValue().get(key);
        redisBoard.incrementViews();
        redisTemplate.opsForValue().set(key,redisBoard);
        // then
        assertThat(redisBoard.getId()).isEqualTo(findBoard.getId());
    }

    @Test
    @DisplayName("게시글 좋아요 V2")
    void 게시글_좋아요_V2() {
        // given
        Board findBoard = boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("게시글이 존재하지 않습니다.")
        );
        String key = "board:" + findBoard.getId();
        Set<Long> memberIds = findBoard.getBoardLikes().stream().map(boardLike -> boardLike.getMember().getId()).collect(Collectors.toSet());
        BoardForRedis boardDto = BoardForRedis.builder()
                .board(findBoard)
                .memberIds(memberIds)
                .build();
        redisTemplate.opsForValue().set(key, boardDto);
        // when
        try {
            BoardForRedis findBoardForRedis = (BoardForRedis) redisTemplate.opsForValue().get(key);
            findBoardForRedis.addLike(memberId);
            redisTemplate.opsForValue().set(key, findBoardForRedis);
            BoardForRedis result = (BoardForRedis) redisTemplate.opsForValue().get(key);
            // then
            assertTrue(result.getLikes().contains(memberId));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
