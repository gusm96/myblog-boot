package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentDeleteReqDto;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.comment.CommentUpdateReqDto;
import com.moya.myblogboot.domain.board.ModificationStatus;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.CommentRepository;
import com.moya.myblogboot.service.CommentService;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class CommentServiceImplTest extends AbstractContainerBaseTest {

    @Autowired private CommentService commentService;
    @Autowired private AdminRepository adminRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BoardRepository boardRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Board testBoard;
    private Comment testComment;
    private static final String COMMENT_PASSWORD = "testPw1234";

    @BeforeEach
    void before() {
        Admin admin = adminRepository.save(Admin.builder()
                .username("commentTestAdmin")
                .password(passwordEncoder.encode("adminPw"))
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("댓글테스트카테고리")
                .build());

        testBoard = boardRepository.save(Board.builder()
                .title("댓글테스트게시글")
                .content("내용")
                .category(category)
                .admin(admin)
                .build());

        testComment = commentRepository.save(Comment.builder()
                .comment("기존 댓글")
                .nickname("tester")
                .discriminator("1234")
                .password(passwordEncoder.encode(COMMENT_PASSWORD))
                .isAdmin(false)
                .board(testBoard)
                .build());
    }

    @Test
    @DisplayName("어드민 댓글 작성 성공")
    void write_admin() {
        CommentReqDto reqDto = new CommentReqDto();
        reqDto.setComment("어드민 댓글");

        assertThat(commentService.write(reqDto, testBoard.getId(), true)).isNotNull();
    }

    @Test
    @DisplayName("비회원 댓글 작성 성공")
    void write_guest() {
        CommentReqDto reqDto = new CommentReqDto();
        reqDto.setComment("비회원 댓글");
        reqDto.setNickname("guest");
        reqDto.setPassword("guestPw!1");

        assertThat(commentService.write(reqDto, testBoard.getId(), false)).isNotNull();
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void write_childComment() {
        CommentReqDto reqDto = new CommentReqDto();
        reqDto.setComment("대댓글입니다.");
        reqDto.setNickname("child");
        reqDto.setPassword("childPw!1");
        reqDto.setParentId(testComment.getId());

        commentService.write(reqDto, testBoard.getId(), false);

        assertThat(testComment.getChild()).hasSize(1);
    }

    @Test
    @DisplayName("어드민 댓글 수정 성공")
    void update_admin() {
        CommentUpdateReqDto reqDto = new CommentUpdateReqDto();
        reqDto.setComment("수정된 댓글");

        commentService.update(testComment.getId(), reqDto, true);

        assertThat(testComment.getComment()).isEqualTo("수정된 댓글");
        assertThat(testComment.getModificationStatus()).isEqualTo(ModificationStatus.MODIFIED);
    }

    @Test
    @DisplayName("비회원 댓글 수정 성공 - 비밀번호 일치")
    void update_guest() {
        CommentUpdateReqDto reqDto = new CommentUpdateReqDto();
        reqDto.setComment("수정된 댓글");
        reqDto.setPassword(COMMENT_PASSWORD);

        commentService.update(testComment.getId(), reqDto, false);

        assertThat(testComment.getComment()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 비밀번호 불일치")
    void update_wrongPassword() {
        CommentUpdateReqDto reqDto = new CommentUpdateReqDto();
        reqDto.setComment("수정된 댓글");
        reqDto.setPassword("wrongPassword");

        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.update(testComment.getId(), reqDto, false));
    }

    @Test
    @DisplayName("어드민 댓글 삭제 성공")
    void delete_admin() {
        commentService.delete(testComment.getId(), new CommentDeleteReqDto(), true);

        assertThrows(EntityNotFoundException.class,
                () -> commentService.retrieve(testComment.getId()));
    }

    @Test
    @DisplayName("비회원 댓글 삭제 성공 - 비밀번호 일치")
    void delete_guest() {
        CommentDeleteReqDto reqDto = new CommentDeleteReqDto();
        reqDto.setPassword(COMMENT_PASSWORD);

        commentService.delete(testComment.getId(), reqDto, false);

        assertThrows(EntityNotFoundException.class,
                () -> commentService.retrieve(testComment.getId()));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 비밀번호 불일치")
    void delete_wrongPassword() {
        CommentDeleteReqDto reqDto = new CommentDeleteReqDto();
        reqDto.setPassword("wrongPassword");

        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.delete(testComment.getId(), reqDto, false));
    }

    @Test
    @DisplayName("게시글의 댓글 목록 조회")
    void retrieveAll() {
        List<CommentResDto> result = commentService.retrieveAll(testBoard.getId());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getComment()).isEqualTo("기존 댓글");
    }
}
