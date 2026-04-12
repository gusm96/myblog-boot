package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.dto.comment.CommentDeleteReqDto;
import com.moya.myblogboot.dto.comment.CommentReqDto;
import com.moya.myblogboot.dto.comment.CommentUpdateReqDto;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.CommentRepository;
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

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AuthService authService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private RestDocumentationResultHandler restDocs;
    @Autowired private ObjectMapper objectMapper;

    private String accessToken;
    private Long parentId;
    private Long postId;

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
        Admin admin = Admin.builder()
                .username("testAdmin")
                .password(passwordEncoder.encode("testPassword"))
                .build();
        Admin saveAdmin = adminRepository.save(admin);

        LoginReqDto loginReqDto = LoginReqDto.builder()
                .username("testAdmin")
                .password("testPassword")
                .build();

        accessToken = "bearer " + authService.adminLogin(loginReqDto).getAccess_token();

        Category category = Category.builder().name("category").build();
        Category saveCategory = categoryRepository.save(category);

        Post post = Post.builder()
                .admin(saveAdmin)
                .category(saveCategory)
                .title("title")
                .content("content")
                .build();
        Post savePost = postRepository.save(post);
        postId = savePost.getId();

        for (int i = 0; i < 5; i++) {
            Comment newComment = Comment.builder()
                    .post(savePost)
                    .nickname("tester")
                    .discriminator("100" + i)
                    .password(passwordEncoder.encode("testPw"))
                    .isAdmin(false)
                    .comment("test")
                    .build();
            Comment saveComment = commentRepository.save(newComment);
            parentId = saveComment.getId();
            for (int j = 0; j < 5; j++) {
                Comment childComment = Comment.builder()
                        .post(savePost)
                        .nickname("child")
                        .discriminator("200" + j)
                        .password(passwordEncoder.encode("testPw"))
                        .isAdmin(false)
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
        ResultActions resultActions = mockMvc.perform(get("/api/v1/comments/{postId}", postId));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("댓글 ID"),
                                fieldWithPath("[].writer").description("작성자 (닉네임#구분자 또는 [관리자])"),
                                fieldWithPath("[].isAdmin").description("관리자 댓글 여부"),
                                fieldWithPath("[].comment").description("댓글 내용"),
                                fieldWithPath("[].createDate").description("작성일"),
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
                                fieldWithPath("[].writer").description("작성자 (닉네임#구분자 또는 [관리자])"),
                                fieldWithPath("[].isAdmin").description("관리자 댓글 여부"),
                                fieldWithPath("[].comment").description("댓글 내용"),
                                fieldWithPath("[].createDate").description("작성일"),
                                fieldWithPath("[].modificationStatus").description("수정 여부 (ORIGINAL / MODIFIED)"),
                                fieldWithPath("[].childCount").description("대댓글 수")
                        )
                ));
    }

    @Test
    @DisplayName("어드민 댓글 작성")
    void writeComment_admin() throws Exception {
        CommentReqDto comment = new CommentReqDto();
        comment.setComment("어드민 댓글입니다.");

        ResultActions resultActions = mockMvc.perform(post("/api/v1/comments/{postId}", postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(comment)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (어드민)")
                        ),
                        requestFields(
                                fieldWithPath("comment").description("댓글 내용 (2~500자)"),
                                fieldWithPath("nickname").description("닉네임 (비회원만)").optional(),
                                fieldWithPath("password").description("비밀번호 (비회원만)").optional(),
                                fieldWithPath("parentId").description("부모 댓글 ID (대댓글)").optional()
                        ),
                        responseFields(
                                fieldWithPath("nickname").description("작성자 닉네임"),
                                fieldWithPath("discriminator").description("구분자 (4자리)")
                        )
                ));
    }

    @Test
    @DisplayName("비회원 댓글 작성")
    void writeComment_guest() throws Exception {
        CommentReqDto comment = new CommentReqDto();
        comment.setComment("비회원 댓글입니다.");
        comment.setNickname("visitor");
        comment.setPassword("visitPw1!");

        ResultActions resultActions = mockMvc.perform(post("/api/v1/comments/{postId}", postId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(comment)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("댓글 수정 - 어드민")
    void editComment() throws Exception {
        CommentUpdateReqDto updateReqDto = new CommentUpdateReqDto();
        updateReqDto.setComment("수정된 댓글입니다.");

        ResultActions resultActions = mockMvc.perform(put("/api/v1/comments/{commentId}", parentId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateReqDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("commentId").description("수정할 댓글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (어드민)")
                        ),
                        requestFields(
                                fieldWithPath("comment").description("수정할 댓글 내용 (2~500자)"),
                                fieldWithPath("password").description("비밀번호 (비회원만)").optional()
                        )
                ));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteCommentNotFound() throws Exception {
        CommentDeleteReqDto deleteReqDto = new CommentDeleteReqDto();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/" + Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(deleteReqDto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("댓글 작성 실패 - 존재하지 않는 게시글")
    void writeCommentWithNonExistentPost() throws Exception {
        CommentReqDto comment = new CommentReqDto();
        comment.setComment("test comment");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/" + Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("댓글 삭제 - 어드민")
    void deleteComment() throws Exception {
        CommentDeleteReqDto deleteReqDto = new CommentDeleteReqDto();

        ResultActions resultActions = mockMvc.perform(delete("/api/v1/comments/{commentId}", parentId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(deleteReqDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("commentId").description("삭제할 댓글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (어드민)")
                        )
                ));
    }
}
