package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardDto;
import com.moya.myblogboot.domain.Category;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@Transactional
class BoardServiceTest {
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    BoardService boardService;
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AdminRepository adminRepository;

    public Admin createAdmin(){
        return Admin.builder()
                .admin_name("moya")
                .admin_pw("moya1343")
                .nickname("Moyada")
                .build();
    }
    @Test
    void findBoardList() {
        int offset = 0;
        int limit = 5;
        List<Board> result = boardRepository.findAll(offset, limit);

        for (Board val : result) {
            System.out.println(val.toString());
        }
    }

    @Test
    void getBoard() {
    }

    @Test
    void editBoard() {
        // given
        Board board = boardRepository.findOne(10L).orElseThrow();
        // when
        // then

    }

    @Test
    void deleteBoard() {
    }

    @Test
    void newPost() {
        Admin admin = adminRepository.findById("moya").orElseThrow(() -> new IllegalStateException("해당 계정은 존재하지 않습니다."));
        Category category = Category.builder().name("Java").build(); // 비영속
        categoryRepository.create(category); // 영속

        BoardDto boardDto = new BoardDto();
        boardDto.setCategory(category);
        boardDto.setTitle("제목");
        boardDto.setContent("내용");

        Board board= Board.builder()
                .admin(admin)
                .title(boardDto.getTitle())
                .content(boardDto.getContent())
                .category(boardDto.getCategory()).build();

        Long result = boardRepository.upload(board);

        Assertions.assertThat(result > 0L);
    }
}