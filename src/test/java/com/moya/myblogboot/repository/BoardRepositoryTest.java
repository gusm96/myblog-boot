package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.board.Board;

import static com.moya.myblogboot.domain.board.QBoard.*;
import static org.assertj.core.api.Assertions.*;

import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SpringBootTest
@Transactional
class BoardRepositoryTest {
    @Autowired
    BoardRepository boardRepository;

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;
    private static final int LIMIT = 4;
    private static final int NOW_PAGE = 0;
    private static final LocalDateTime CURRENT_DATE = LocalDateTime.now();
    private static final LocalDateTime DELETE_DATE = CURRENT_DATE.minus(16, ChronoUnit.DAYS);// 16일을 뺀 날짜 및 시간

    @BeforeEach
    void before (){
        Member member = Member.builder().username("member1").password("member1").nickname("member1").build();
        em.persist(member);
        Category category = Category.builder().name("test").build();
        Board board1 = Board.builder()
                .title("test")
                .content("test")
                .category(category)
                .member(member)
                .build();
        Board board2 = Board.builder()
                .title("test")
                .content("test")
                .category(category)
                .member(member)
                .build();
        Board board3 = Board.builder()
                .title("test")
                .content("test")
                .category(category)
                .member(member)
                .build();
        category.addBoard(board1);
        category.addBoard(board2);
        category.addBoard(board3);
        em.persist(category);
        em.persist(board1);
        em.persist(board2);
        em.persist(board3);
        em.flush();
        em.clear();
    }

    @Test
    void findById() {
        Long boardId = 1L;

        Board board = boardRepository.findById(boardId).get();

        assertThat(boardId).isEqualTo(board.getId());
    }

    @Test
    void findAll() {
        //given
        PageRequest pageRequest = PageRequest.of(NOW_PAGE, LIMIT, Sort.by(Sort.Direction.DESC, "uploadDate"));
        //when
        Page<Board> boards = boardRepository.findAll(pageRequest);
        Long count = boardRepository.count();
        //then
        assertThat(LIMIT).isEqualTo(boards.getSize());
        assertThat(count).isEqualTo(boards.getTotalElements());
    }

   @Test
   @DisplayName("카테고리로 게시글 리스트 조회")
   void findAllByCategoryName () {
       // given

       // when

       // then
   }
}