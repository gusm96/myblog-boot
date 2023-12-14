package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.CommentRepository;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    AuthService authService;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    BoardRepository boardRepository;

    private static String accessToken;

    private static Long parentId;

    private static Long boardId;

    @BeforeEach
    void before() {
        Member member = Member.builder()
                .username("testMember")
                .password(passwordEncoder.encode("testPassword"))
                .nickname("testMember")
                .build();
        member.addRoleAdmin();
        Member saveMember = memberRepository.save(member);

        MemberLoginReqDto loginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        // Login 후 Token 발급
        accessToken = "bearer " + authService.memberLogin(loginReqDto).getAccess_token();

        // 카테고리 생성
        Category category = Category.builder().name("category").build();
        Category saveCategory = categoryRepository.save(category);

        // 게시글 생성
        Board board = Board.builder()
                .member(saveMember)
                .category(saveCategory)
                .title("title")
                .content("content")
                .build();
        Board saveBoard = boardRepository.save(board);
        boardId = saveBoard.getId();

        for (int i = 0; i < 5; i++) {
            Comment newComment = Comment.builder()
                    .board(saveBoard)
                    .member(saveMember)
                    .comment("test")
                    .build();
            Comment saveComment = commentRepository.save(newComment);
            
            // 부모 댓글 아이디로 사용할 예정
            parentId = saveComment.getId();
        }
    }
    @Test
    @DisplayName("댓글 리스트 조회")
    void requestCommentList() throws Exception {
        // given
        String path = "/api/v1/comments/" + boardId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("댓글 작성")
    void requestCreateComment() throws Exception {
        // given
        String path = "/api/v1/comments/" + boardId;
        CommentReqDto comment1 = new CommentReqDto();
        comment1.setComment("댓글 입니다.");

        CommentReqDto comment2 = new CommentReqDto("댓글 입니다.", parentId);
        // when
        // 댓글 작성
        ResultActions resultActions1 = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json").content(new ObjectMapper().writeValueAsString(comment1)));
        // 대댓글 작성
        ResultActions resultActions2 = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json").content(new ObjectMapper().writeValueAsString(comment2)));
        // then
        // 댓글 결과
        resultActions1.andExpect(MockMvcResultMatchers.status().isOk());
        // 대댓글 결과
        resultActions2.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("댓글 수정")
    void requestEditComment() throws Exception{
        // given
        String path = "/api/v1/comments/" + parentId;
        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setComment("댓글 수정");
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.put(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json").content(new ObjectMapper().writeValueAsString(commentReqDto)));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("댓글 삭제")
    void requestToDeleteComment() throws Exception {
        // given
        String path = "/api/v1/comments/" + parentId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }
}