package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;
import static org.assertj.core.api.Assertions.*;

import com.moya.myblogboot.domain.category.Category;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class BoardRepositoryTest {
    private static final int limit = 4;
    private static final int nowPage = 0;

   /* @Test
    void findById() {
        Long boardId = 1L;

        Board board = boardRepository.findById(boardId).get();

        assertThat(boardId).isEqualTo(board.getId());
    }
    @Test
    void findAll() {
        //given
        PageRequest pageRequest = PageRequest.of(nowPage, limit, Sort.by(Sort.Direction.DESC, "uploadDate"));
        //when
        Page<Board> boards = boardRepository.findAll(pageRequest);
        Long count = boardRepository.count();
        //then
        assertThat(limit).isEqualTo(boards.getSize());
        assertThat(count).isEqualTo(boards.getTotalElements());
    }

    @Test
    void findAllByCategory() {
        //given
        Category category = categoryRepository.findById(1L).get();
        PageRequest pageRequest = PageRequest.of(nowPage, limit, Sort.by(Sort.Direction.DESC, "uploadDate"));
        //when
        Page<Board> boards = boardRepository.findAllByCategory(category, pageRequest);
        for (Board board : boards) {
            System.out.println(board.getId());
        }

        //then
        assertThat(limit).isEqualTo(boards.getSize());
    }

    @Test
    void findAllBySearchType() {
        //given
        PageRequest pageRequest = PageRequest.of(nowPage, limit, Sort.by(Sort.Direction.DESC, "uploadDate"));

    }*/
}