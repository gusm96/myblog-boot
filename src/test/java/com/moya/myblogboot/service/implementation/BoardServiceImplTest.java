package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardStatus;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.dto.board.BoardListResDto;
import com.moya.myblogboot.dto.board.BoardReqDto;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.BoardService;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class BoardServiceImplTest {

    @Autowired private BoardService boardService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BoardRepository boardRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member testMember;
    private Member anotherMember;
    private Category testCategory;
    private Board testBoard;

    @BeforeEach
    void before() {
        testMember = memberRepository.save(Member.builder()
                .username("boardTestUser")
                .password(passwordEncoder.encode("testPw"))
                .nickname("boardTester")
                .build());

        anotherMember = memberRepository.save(Member.builder()
                .username("boardAnotherUser")
                .password(passwordEncoder.encode("testPw"))
                .nickname("boardAnother")
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("게시글테스트카테고리")
                .build());

        testBoard = boardRepository.save(Board.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(testCategory)
                .member(testMember)
                .build());
    }

    @Test
    @DisplayName("게시글 목록 조회")
    void retrieveAll() {
        BoardListResDto result = boardService.retrieveAll(0);

        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    @DisplayName("카테고리별 게시글 목록 조회")
    void retrieveAllByCategory() {
        BoardListResDto result = boardService.retrieveAllByCategory(testCategory.getName(), 0);

        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void write() {
        BoardReqDto reqDto = BoardReqDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .category(testCategory.getId())
                .build();

        Long boardId = boardService.write(reqDto, testMember.getId());

        assertThat(boardId).isNotNull();
        assertThat(boardRepository.findById(boardId)).isPresent();
    }

    @Test
    @DisplayName("게시글 작성 실패 - 존재하지 않는 카테고리")
    void write_invalidCategory() {
        BoardReqDto reqDto = BoardReqDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .category(999L)
                .build();

        assertThrows(EntityNotFoundException.class,
                () -> boardService.write(reqDto, testMember.getId()));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void edit() {
        BoardReqDto modifiedDto = BoardReqDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .category(testCategory.getId())
                .build();

        Long result = boardService.edit(testMember.getId(), testBoard.getId(), modifiedDto);

        assertThat(result).isEqualTo(testBoard.getId());
        assertThat(testBoard.getTitle()).isEqualTo("수정된 제목");
        assertThat(testBoard.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void edit_unauthorized() {
        BoardReqDto modifiedDto = BoardReqDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .category(testCategory.getId())
                .build();

        assertThrows(UnauthorizedAccessException.class,
                () -> boardService.edit(anotherMember.getId(), testBoard.getId(), modifiedDto));
    }

    @Test
    @DisplayName("게시글 소프트 삭제")
    void delete() {
        boardService.delete(testBoard.getId(), testMember.getId());

        assertThat(testBoard.getBoardStatus()).isEqualTo(BoardStatus.HIDE);
        assertThat(testBoard.getDeleteDate()).isNotNull();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void delete_unauthorized() {
        assertThrows(UnauthorizedAccessException.class,
                () -> boardService.delete(testBoard.getId(), anotherMember.getId()));
    }

    @Test
    @DisplayName("삭제된 게시글 복원")
    void undelete() {
        boardService.delete(testBoard.getId(), testMember.getId());
        boardService.undelete(testBoard.getId(), testMember.getId());

        assertThat(testBoard.getBoardStatus()).isEqualTo(BoardStatus.VIEW);
        assertThat(testBoard.getDeleteDate()).isNull();
    }

    @Test
    @DisplayName("게시글 조회 실패 - 존재하지 않는 ID")
    void findById_notFound() {
        assertThrows(EntityNotFoundException.class,
                () -> boardService.findById(999L));
    }

    @Test
    @DisplayName("조회수 중복 검사 - 최초 접근 시 false 반환")
    void isDuplicateBoardViewCount_firstAccess() {
        String key = "board:view:test:" + UUID.randomUUID();

        boolean result = boardService.isDuplicateBoardViewCount(key);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("조회수 중복 검사 - 재접근 시 true 반환")
    void isDuplicateBoardViewCount_duplicate() {
        String key = "board:view:test:" + UUID.randomUUID();
        boardService.isDuplicateBoardViewCount(key);

        boolean result = boardService.isDuplicateBoardViewCount(key);

        assertThat(result).isTrue();
    }
}
