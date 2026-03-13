package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
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

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class CommentControllerTest extends AbstractContainerBaseTest {

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
    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long parentId;
    private Long boardId;

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

        accessToken = "bearer " + authService.memberLogin(loginReqDto).getAccess_token();

        Category category = Category.builder().name("category").build();
        Category saveCategory = categoryRepository.save(category);

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
            parentId = saveComment.getId();
            for (int j = 0; j < 5; j++) {
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
        ResultActions resultActions = mockMvc.perform(get("/api/v1/comments/{boardId}", boardId));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("boardId").description("게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("댓글 ID"),
                                fieldWithPath("[].writer").description("작성자 닉네임"),
                                fieldWithPath("[].comment").description("댓글 내용"),
                                fieldWithPath("[].write_date").description("작성일"),
                                fieldWithPath("[].modificationStatus").description("수정 여부 (ORIGINAL / MODIFIED)"),
                                fieldWithPath("[].childCount").description("대댓글 수")
                        )
                ));
    }

    @Test
    @DisplayName("자식 댓글 리스트 조회")
    void getChildComments() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/api/v1/comments/child/{parentId}", parentId));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("parentId").description("부모 댓글 ID")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("댓글 ID"),
                                fieldWithPath("[].writer").description("작성자 닉네임"),
                                fieldWithPath("[].comment").description("댓글 내용"),
                                fieldWithPath("[].write_date").description("작성일"),
                                fieldWithPath("[].modificationStatus").description("수정 여부 (ORIGINAL / MODIFIED)"),
                                fieldWithPath("[].childCount").description("대댓글 수")
                        )
                ));
    }

    @Test
    @DisplayName("댓글 작성")
    void writeComment() throws Exception {
        CommentReqDto comment = new CommentReqDto();
        comment.setComment("Comments");

        ResultActions resultActions = mockMvc.perform(post("/api/v1/comments/{boardId}", boardId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(comment)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("boardId").description("게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token")
                        ),
                        requestFields(
                                fieldWithPath("comment").description("댓글 내용 (2~500자)"),
                                fieldWithPath("parentId").description("부모 댓글 ID (대댓글인 경우)").optional()
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("댓글 수정")
    void editComment() throws Exception {
        CommentReqDto commentReqDto = new CommentReqDto();
        commentReqDto.setComment("Modified Comment");

        ResultActions resultActions = mockMvc.perform(put("/api/v1/comments/{commentId}", parentId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(commentReqDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("commentId").description("수정할 댓글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token")
                        ),
                        requestFields(
                                fieldWithPath("comment").description("수정할 댓글 내용 (2~500자)"),
                                fieldWithPath("parentId").description("부모 댓글 ID").optional()
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteCommentNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/" + Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("댓글 작성 실패 - 존재하지 않는 게시글")
    void writeCommentWithNonExistentBoard() throws Exception {
        CommentReqDto comment = new CommentReqDto();
        comment.setComment("test comment");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/" + Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("댓글 삭제")
    void deleteComment() throws Exception {
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/comments/{commentId}", parentId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("commentId").description("삭제할 댓글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token")
                        )
                ));
    }
}
