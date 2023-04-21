package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardInfo;
import com.moya.myblogboot.repository.BoardRepository;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
class BoardServiceTest {
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    BoardService boardService;
    @Test
    void findAllPosts() {
        int offset = 0;
        int limit = 5;
        List<Board> result = boardRepository.findAllPosts(offset, limit);

        for (Board val : result) {
            System.out.println(val.toString());
        }
    }

    @Test
    void getBoard() {
    }

    @Test
    void editBoard() {
    }

    @Test
    void deleteBoard() {
    }

    @Test
    void newPost() {
        Board board = Board.builder()
                .aidx(0)
                .board_type(0)
                .title("제목")
                .content("내용")
                .build();

        long result = boardRepository.upload(board);

        assertThat(result).isNotZero();
    }
}