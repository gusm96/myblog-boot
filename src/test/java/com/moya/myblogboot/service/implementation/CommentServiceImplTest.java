package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.board.ModificationStatus;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.CommentRepository;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.CommentService;
import jakarta.persistence.EntityNotFoundException;
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
class CommentServiceImplTest {

    @Autowired private CommentService commentService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BoardRepository boardRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member testMember;
    private Member anotherMember;
    private Board testBoard;
    private Comment testComment;

    @BeforeEach
    void before() {
        testMember = memberRepository.save(Member.builder()
                .username("commentTestUser")
                .password(passwordEncoder.encode("testPw"))
                .nickname("commentTester")
                .build());

        anotherMember = memberRepository.save(Member.builder()
                .username("anotherCommentUser")
                .password(passwordEncoder.encode("testPw"))
                .nickname("anotherCommenter")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("댓글테스트카테고리")
                .build());

        testBoard = boardRepository.save(Board.builder()
                .title("댓글테스트게시글")
                .content("내용")
                .category(category)
                .member(testMember)
                .build());

        testComment = commentRepository.save(Comment.builder()
                .comment("기존 댓글")
                .member(testMember)
                .board(testBoard)
                .build());
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void write() {
        CommentReqDto reqDto = new CommentReqDto();
        reqDto.setComment("새로운 댓글");

        String result = commentService.write(reqDto, testMember.getId(), testBoard.getId());

        assertThat(result).isEqualTo("댓글이 등록되었습니다.");
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void write_childComment() {
        CommentReqDto reqDto = new CommentReqDto();
        reqDto.setComment("대댓글입니다.");
        reqDto.setParentId(testComment.getId());

        String result = commentService.write(reqDto, testMember.getId(), testBoard.getId());

        assertThat(result).isEqualTo("댓글이 등록되었습니다.");
        assertThat(testComment.getChild()).hasSize(1);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void update() {
        String result = commentService.update(testComment.getId(), testMember.getId(), "수정된 댓글");

        assertThat(result).isEqualTo("댓글이 수정되었습니다.");
        assertThat(testComment.getComment()).isEqualTo("수정된 댓글");
        assertThat(testComment.getModificationStatus()).isEqualTo(ModificationStatus.MODIFIED);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 권한 없음")
    void update_unauthorized() {
        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.update(testComment.getId(), anotherMember.getId(), "수정된 댓글"));
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void delete() {
        String result = commentService.delete(testComment.getId(), testMember.getId());

        assertThat(result).isEqualTo("댓글이 삭제되었습니다.");
        assertThrows(EntityNotFoundException.class,
                () -> commentService.retrieve(testComment.getId()));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 권한 없음")
    void delete_unauthorized() {
        assertThrows(UnauthorizedAccessException.class,
                () -> commentService.delete(testComment.getId(), anotherMember.getId()));
    }

    @Test
    @DisplayName("게시글의 댓글 목록 조회")
    void retrieveAll() {
        List<CommentResDto> result = commentService.retrieveAll(testBoard.getId());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getComment()).isEqualTo("기존 댓글");
    }
}
