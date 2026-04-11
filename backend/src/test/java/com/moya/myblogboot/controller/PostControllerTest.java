package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.constants.CookieName;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostReqDto;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.CategoryRepository;
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
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class PostControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private RestDocumentationResultHandler restDocs;
    @Autowired
    private ObjectMapper objectMapper;

    private Long postId;
    private String postSlug;
    private Long categoryId;
    private String accessToken;

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
                .username("testMember")
                .password(passwordEncoder.encode("testPassword"))
                .build();
        Admin saveAdmin = adminRepository.save(admin);
        Category category = Category.builder().name("Test").build();
        Category saveCategory = categoryRepository.save(category);
        categoryId = saveCategory.getId();

        for (int i = 0; i < 5; i++) {
            Post newPost = Post.builder()
                    .admin(saveAdmin)
                    .category(saveCategory)
                    .title("title")
                    .content("content")
                    .slug("test-title-" + i)
                    .build();
            Post result = postRepository.save(newPost);
            postId = result.getId();
            postSlug = "test-title-" + i;
        }

        MemberLoginReqDto loginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        accessToken = "bearer " + authService.adminLogin(loginReqDto).getAccess_token();
    }

    @Test
    @DisplayName("모든 게시글 조회")
    void getAllPosts() throws Exception {
        int page = 1;
        String path = "/api/v1/posts";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("p", String.valueOf(page)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("p").description("페이지 번호 (기본값: 1)")
                        ),
                        responseFields(
                                fieldWithPath("list").description("게시글 목록"),
                                fieldWithPath("list[].id").description("게시글 ID"),
                                fieldWithPath("list[].title").description("게시글 제목"),
                                fieldWithPath("list[].content").description("게시글 내용"),
                                fieldWithPath("list[].slug").description("게시글 슬러그"),
                                fieldWithPath("list[].createDate").description("작성일"),
                                fieldWithPath("list[].updateDate").description("수정일").optional().type(STRING),
                                fieldWithPath("list[].deleteDate").description("삭제 예정일").optional().type(STRING),
                                fieldWithPath("list[].postStatus").description("게시글 상태 (VIEW / HIDE)"),
                                fieldWithPath("totalPage").description("전체 페이지 수")
                        )
                ));
    }

    @Test
    @DisplayName("카테고리별 게시글 조회")
    void getCategoryPosts() throws Exception {
        int page = 1;
        String category = "Test";
        String path = "/api/v1/posts/category";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("c", category)
                .param("p", String.valueOf(page)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("c").description("카테고리명"),
                                parameterWithName("p").description("페이지 번호 (기본값: 1)")
                        ),
                        responseFields(
                                fieldWithPath("list").description("게시글 목록"),
                                fieldWithPath("list[].id").description("게시글 ID"),
                                fieldWithPath("list[].title").description("게시글 제목"),
                                fieldWithPath("list[].content").description("게시글 내용"),
                                fieldWithPath("list[].slug").description("게시글 슬러그"),
                                fieldWithPath("list[].createDate").description("작성일"),
                                fieldWithPath("list[].updateDate").description("수정일").optional().type(STRING),
                                fieldWithPath("list[].deleteDate").description("삭제 예정일").optional().type(STRING),
                                fieldWithPath("list[].postStatus").description("게시글 상태"),
                                fieldWithPath("totalPage").description("전체 페이지 수")
                        )
                ));
    }

    @Test
    @DisplayName("검색 결과별 게시글 조회")
    void getSearchedPosts() throws Exception {
        SearchType searchType = SearchType.TITLE;
        String contents = "title";
        int page = 1;
        String path = "/api/v1/posts/search";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("p", String.valueOf(page))
                .param("type", String.valueOf(searchType))
                .param("contents", contents));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("type").description("검색 타입 (TITLE / CONTENT)"),
                                parameterWithName("contents").description("검색어"),
                                parameterWithName("p").description("페이지 번호 (기본값: 1)")
                        ),
                        responseFields(
                                fieldWithPath("list").description("검색된 게시글 목록"),
                                fieldWithPath("list[].id").description("게시글 ID"),
                                fieldWithPath("list[].title").description("게시글 제목"),
                                fieldWithPath("list[].content").description("게시글 내용"),
                                fieldWithPath("list[].slug").description("게시글 슬러그"),
                                fieldWithPath("list[].createDate").description("작성일"),
                                fieldWithPath("list[].updateDate").description("수정일").optional().type(STRING),
                                fieldWithPath("list[].deleteDate").description("삭제 예정일").optional().type(STRING),
                                fieldWithPath("list[].postStatus").description("게시글 상태"),
                                fieldWithPath("totalPage").description("전체 페이지 수")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 (slug) — 최초 조회: 조회수 증가 + viewed_posts 쿠키 발급")
    void getPostDetailV1BySlug_최초조회() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(postId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.slug").value(postSlug))
                .andExpect(MockMvcResultMatchers.cookie().exists("viewed_posts"))
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("identifier").description("게시글 슬러그 또는 숫자 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시글 ID"),
                                fieldWithPath("title").description("게시글 제목"),
                                fieldWithPath("content").description("게시글 내용"),
                                fieldWithPath("views").description("조회수"),
                                fieldWithPath("likes").description("좋아요 수"),
                                fieldWithPath("createDate").description("작성일"),
                                fieldWithPath("updateDate").description("수정일").optional().type(STRING),
                                fieldWithPath("deleteDate").description("삭제 예정일").optional().type(STRING),
                                fieldWithPath("postStatus").description("게시글 상태"),
                                fieldWithPath("slug").description("게시글 슬러그"),
                                fieldWithPath("categoryName").description("카테고리명")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 (slug) — 중복 조회: viewed_posts 쿠키 포함 시 조회수 증가 없음")
    void getPostDetailV1BySlug_중복조회() throws Exception {
        // 1차 조회로 쿠키 발급
        MvcResult firstResult = mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Cookie viewedCookie = firstResult.getResponse().getCookie("viewed_posts");
        assertThat(viewedCookie).isNotNull();

        long viewsAfterFirst = ((Number) new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(firstResult.getResponse().getContentAsString())
                .get("views").numberValue()).longValue();

        // 2차 조회 (동일 쿠키 포함)
        MvcResult secondResult = mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug)
                        .cookie(viewedCookie))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        long viewsAfterSecond = ((Number) new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(secondResult.getResponse().getContentAsString())
                .get("views").numberValue()).longValue();

        assertThat(viewsAfterSecond).isEqualTo(viewsAfterFirst);
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 (ID) — 최초 조회: 조회수 증가 + viewed_posts 쿠키 발급")
    void getPostDetailV1ById_최초조회() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/api/v1/posts/{identifier}", postId));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(postId))
                .andExpect(MockMvcResultMatchers.cookie().exists("viewed_posts"));
    }

    @Test
    @DisplayName("게시글 상세 조회 V8 → V1 301 redirect")
    void getPostDetailV8Redirect() throws Exception {
        mockMvc.perform(get("/api/v8/posts/{postId}", postId))
                .andExpect(MockMvcResultMatchers.status().isMovedPermanently())
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.LOCATION, "/api/v1/posts/" + postId))
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID (v1으로 영구 이전됨)")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("이전된 엔드포인트 URI")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 실패 - 존재하지 않는 슬러그")
    void getPostDetailV1NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{identifier}", "nonexistent-slug"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("게시글 상세 관리자용")
    void getPostDetailForAdmin() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/api/v1/management/posts/{postId}", postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        responseFields(
                                fieldWithPath("id").description("게시글 ID"),
                                fieldWithPath("title").description("게시글 제목"),
                                fieldWithPath("content").description("게시글 내용"),
                                fieldWithPath("views").description("조회수"),
                                fieldWithPath("likes").description("좋아요 수"),
                                fieldWithPath("createDate").description("작성일"),
                                fieldWithPath("updateDate").description("수정일").optional().type(STRING),
                                fieldWithPath("deleteDate").description("삭제 예정일").optional().type(STRING),
                                fieldWithPath("postStatus").description("게시글 상태"),
                                fieldWithPath("slug").description("게시글 슬러그"),
                                fieldWithPath("categoryName").description("카테고리명")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 등록")
    void writePost() throws Exception {
        String path = "/api/v1/posts";
        PostReqDto postReqDto = PostReqDto.builder()
                .category(categoryId)
                .title("title")
                .content("content")
                .build();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(postReqDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        requestFields(
                                fieldWithPath("title").description("게시글 제목 (2~45자)"),
                                fieldWithPath("content").description("게시글 내용"),
                                fieldWithPath("category").description("카테고리 ID"),
                                fieldWithPath("images").description("첨부 이미지 목록").optional().type(STRING),
                                fieldWithPath("slug").description("슬러그 (미입력 시 제목에서 자동 생성)").optional().type(STRING),
                                fieldWithPath("metaDescription").description("메타 설명 (최대 160자)").optional().type(STRING),
                                fieldWithPath("metaKeywords").description("메타 키워드").optional().type(STRING),
                                fieldWithPath("thumbnailUrl").description("대표 이미지 URL").optional().type(STRING)
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("게시글 수정")
    void editPost() throws Exception {
        PostReqDto modifiedPostDto = PostReqDto.builder()
                .title("modifiedTitle")
                .content("modifiedContent")
                .category(categoryId)
                .build();

        ResultActions resultActions = mockMvc.perform(put("/api/v1/posts/{postId}", postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(modifiedPostDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("수정할 게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        requestFields(
                                fieldWithPath("title").description("수정할 제목 (2~45자)"),
                                fieldWithPath("content").description("수정할 내용"),
                                fieldWithPath("category").description("카테고리 ID"),
                                fieldWithPath("images").description("첨부 이미지 목록").optional().type(STRING),
                                fieldWithPath("slug").description("수정할 슬러그 (미입력 시 기존 슬러그 유지)").optional().type(STRING),
                                fieldWithPath("metaDescription").description("수정할 메타 설명").optional().type(STRING),
                                fieldWithPath("metaKeywords").description("수정할 메타 키워드").optional().type(STRING),
                                fieldWithPath("thumbnailUrl").description("수정할 대표 이미지 URL").optional().type(STRING)
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("게시글 삭제")
    void deletePost() throws Exception {
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("삭제할 게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        )
                ));
    }

    @Test
    @DisplayName("삭제 예정 게시글 리스트")
    void getDeletedPosts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/" + postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        String path = "/api/v1/deleted-posts";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .param("p", "1")
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("p").description("페이지 번호 (기본값: 1)")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 삭제 취소")
    void cancelDeletedPost() throws Exception {
        // 먼저 삭제 요청
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/" + postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        ResultActions resultActions = mockMvc.perform(put("/api/v1/deleted-posts/{postId}", postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("복원할 게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 영구 삭제")
    void deletePostPermanently() throws Exception {
        // 먼저 삭제(soft-delete) 처리 후 영구 삭제
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/" + postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        ResultActions resultActions = mockMvc.perform(delete("/api/v1/deleted-posts/{postId}", postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("영구 삭제할 게시글 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        )
                ));
    }

    @Test
    @DisplayName("게시글 좋아요")
    void addPostLike() throws Exception {
        ResultActions resultActions = mockMvc.perform(post("/api/v2/likes/{postId}", postId));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.cookie().exists(CookieName.LIKED_POSTS))
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("좋아요할 게시글 ID")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("게시글 좋아요 여부 체크")
    void checkPostLike() throws Exception {
        // 좋아요 추가 후 발급된 쿠키 획득
        MvcResult likeResult = mockMvc.perform(post("/api/v2/likes/{postId}", postId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        Cookie likedCookie = likeResult.getResponse().getCookie(CookieName.LIKED_POSTS);
        assertThat(likedCookie).isNotNull();

        ResultActions resultActions = mockMvc.perform(get("/api/v2/likes/{postId}", postId)
                .cookie(likedCookie));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("true"))
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("게시글 등록 실패 - 인증 없음")
    void writePostWithoutAuth() throws Exception {
        PostReqDto postReqDto = PostReqDto.builder()
                .category(categoryId)
                .title("title")
                .content("content")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(postReqDto)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @DisplayName("게시글 등록 실패 - 유효성 검사 오류 (제목 누락)")
    void writePostWithInvalidInput() throws Exception {
        PostReqDto postReqDto = PostReqDto.builder()
                .category(categoryId)
                .title("")
                .content("content")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(postReqDto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("전체 Slug 목록 조회 — VIEW 상태 게시글만 반환")
    void getAllSlugs() throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/slugs"));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].slug").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].updateDate").exists())
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", "public, max-age=3600"))
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("[].slug").description("게시글 슬러그"),
                                fieldWithPath("[].updateDate").description("최종 수정일")
                        )
                ));
    }

    @Test
    @DisplayName("전체 Slug 목록 조회 — HIDE 상태 게시글 제외 확인")
    void getAllSlugs_excludesHiddenPosts() throws Exception {
        // 게시글 하나를 HIDE로 변경
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/" + postId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/slugs"));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(4));
    }

    @Test
    @DisplayName("게시글 좋아요 취소")
    void cancelPostLike() throws Exception {
        // 좋아요 추가 후 발급된 쿠키 획득
        MvcResult likeResult = mockMvc.perform(post("/api/v2/likes/{postId}", postId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        Cookie likedCookie = likeResult.getResponse().getCookie(CookieName.LIKED_POSTS);
        assertThat(likedCookie).isNotNull();

        ResultActions resultActions = mockMvc.perform(delete("/api/v2/likes/{postId}", postId)
                .cookie(likedCookie));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("postId").description("좋아요 취소할 게시글 ID")
                        ),
                        responseBody()
                ));
    }
}
