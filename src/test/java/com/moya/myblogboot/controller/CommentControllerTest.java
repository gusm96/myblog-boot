package com.moya.myblogboot.controller;

import com.moya.myblogboot.config.RestDocsConfiguration;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private RestDocumentationResultHandler restDocs;

    private static String accessToken;

    private static Long parentId;

    private static Long boardId;

    // REST docs setUp
    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentationContextProvider) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentationContextProvider))
                .apply(springSecurity())
                .alwaysDo(restDocs)
                .build();
    }

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
            // 자식 댓글
            for(int j = 0; j < 5; j++){
                Comment childComment = Comment.builder()
                        .board(saveBoard)
                        .member(saveMember)
                        .comment("child")
                        .build();
                childComment.addParentComment(saveComment);
                Comment saveChildComment = commentRepository.save(childComment);
                saveComment.addChildComment(saveChildComment);
            }
        }
    }

    @Test
    @DisplayName("댓글 리스트 조회")
    void getComments() throws Exception {
        // given
        String path = "/api/v1/comments/" + boardId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("자식 댓글 리스트 조회")
    void getChildComments()throws Exception {
        // given
        String path = "/api/v1/comments/child/" + parentId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("댓글 작성")
    void writeComment() throws Exception {
        // given
        String path = "/api/v1/comments/" + boardId;
        CommentReqDto comment = new CommentReqDto();
        comment.setComment("Comments");

        // when
        // 댓글 작성
        ResultActions resultActions1 = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json").content(new ObjectMapper().writeValueAsString(comment)));

        // then
        // 댓글 결과
        resultActions1.andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    @DisplayName("댓글 수정")
    void editComment() throws Exception {
        // given
        String path = "/api/v1/comments/" + parentId;
        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setComment("Modified Comment");
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.put(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json").content(new ObjectMapper().writeValueAsString(commentReqDto)));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("댓글 삭제")
    void deleteComment() throws Exception {
        // given
        String path = "/api/v1/comments/" + parentId;
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }
}