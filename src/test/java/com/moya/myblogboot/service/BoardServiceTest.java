package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.domain.guest.Guest;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
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
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    AdminRepository adminRepository;
    @Autowired
    InitDb initDb;
    /*public Admin createAdmin(){
        Admin admin = Admin.builder()
                .admin_name("moya")
                .admin_pw("moya1343")
                .nickname("Moyada")
                .build();
        adminRepository.save(admin);
        return admin;
    }
    public Category createCategory(String name) {
        Category category = Category.builder().name(name).build();
        categoryRepository.create(category);
        return category;
    }

    public Board createBoard( Category category, String title, String content) {
        Board board = Board.builder()
                .category(category)
                .title(title)
                .content(content)
                .build();
        boardRepository.upload(board);
        return board;
    }
    @DisplayName("게시글 작성")
    @Test
    void 게시글_작성() {
        // given
        BoardReqDto boardReqDto = BoardReqDto.builder().category(1L).title("제목").content("내용").build();


        Board board = Board.builder()
                .category(categoryRepository.findOne(boardReqDto.getCategory()).orElseThrow(() -> new IllegalStateException("없는 카테고리")))
                .title(boardReqDto.getTitle())
                .content(boardReqDto.getTitle())
                .build();

        // when
        Long result = boardRepository.upload(board);
        // then
        assertThat(result).isEqualTo(board.getId());
    }
    @DisplayName("게시글 리스트")
    @Test
    void 게시글_리스트() {
        // given
        Category category = createCategory("Java");
        Board board1 = createBoard(category, "제목1", "내용1");
        Board board2 = createBoard(category, "제목2", "내용2");
        Board board3 = createBoard(category, "제목3", "내용3");
        int offset = 0;
        int limit = 5;
        // when
        List<Board> findBoards = boardRepository.findAll(offset,limit);
        // then
        assertThat(findBoards.size()).isEqualTo(3);
    }

   @DisplayName("해당 게시글 상세정보")
   @Test
   void 게시글_상세보기() {
       // given
       //Admin admin = createAdmin();
       //Category category = createCategory("java");
      // Board board = createBoard(category,"제목", "내용");
       // when
       Board result = boardRepository.findOne(1L)
               .orElseThrow(() -> new IllegalStateException("해당 게시글이 존재하지 않습니다"));
       // then
       assertThat(result.getId()).isEqualTo(1L);
   }
    @DisplayName("게시글 수정")
    @Test
    @Rollback(value = false)
    void 게시글_수정() {
        // given
        Admin admin = createAdmin(); // 영속
        Category category = createCategory("Java"); // 영속
        Board board = createBoard( category, "제목", "내용"); // 영속
        // when
        Category newCategory = createCategory("Python");
        board.updateBoard(newCategory, "파이썬", "웹크롤링");

        // then
        assertThat(board.getTitle()).isEqualTo("파이썬");

    }
    @DisplayName("게시글 숨김")
    @Test
    @Rollback(value = false)
    void 게시글_숨김() {
        // given
        Admin admin = createAdmin();
        Category category = createCategory("Java");
        Board board = createBoard( category, "제목", "내용");
        // when
        board.updateBoardStatus(BoardStatus.HIDE);
        // then
        assertThat(board.getBoardStatus()).isEqualTo(BoardStatus.HIDE);
    }*/

    @DisplayName("게시글 검색 기능")
    @Test
    void 게시글_검색 () {
        // given
        // init database
        initDb.init();
        // 검색 옵션
        SearchType type = SearchType.TITLE;
        String searchContents = "제목";
        // when
        List<Board> result = boardRepository.findBySearch(type, searchContents, 1, 5);
        // then
        assertThat(result).isNotEmpty();
    }

    @DisplayName("게시글 좋아요")
    @Test
    void 좋아요() {
        // given

        // when

        // then
    }
}