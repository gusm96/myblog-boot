package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.repository.BoardRepository;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BoardServiceTest {
    @Autowired
    BoardRepository boardRepository;
    @Test
    void getRecentPosts() {
        List<Board> result = boardRepository.findRecentPosts();

        assertThat(result).isNotEmpty();
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
        Board board = new Board();
        int aidx = 0;
        int board_type = 0;
        String title = "제목";
        String content = "내용";
        board.setBoard_type(board_type);
        board.setAidx(aidx);
        board.setTitle(title);
        board.setContent(content);

        long result = boardRepository.upload(board);

        assertThat(result).isNotZero();
    }
}