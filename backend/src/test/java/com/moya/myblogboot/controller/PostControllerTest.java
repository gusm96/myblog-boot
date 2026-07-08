package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.constants.CookieName;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.dto.post.PostReqDto;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.service.AuthService;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class PostControllerTest extends AbstractContainerBaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PostRepository postRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;
    @Autowired private AdminRepository adminRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private RestDocumentationResultHandler restDocs;
    @Autowired private ObjectMapper objectMapper;

    private Long postId;
    private String postSlug;
    private String accessToken;
    private Admin admin;
    private Tag tag;

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
        admin = adminRepository.save(Admin.builder()
                .username("testMember")
                .password(passwordEncoder.encode("testPassword"))
                .build());
        tag = tagRepository.save(Tag.builder().name("Test").slug("test").build());

        for (int i = 0; i < 5; i++) {
            Post newPost = Post.builder()
                    .admin(admin)
                    .title("title")
                    .content("content")
                    .slug("test-title-" + i)
                    .build();
            newPost.replaceTags(List.of(tag));
            tag.incrementPostCount();
            Post result = postRepository.save(newPost);
            postId = result.getId();
            postSlug = result.getSlug();
        }

        accessToken = authService.adminLogin(LoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build()).accessToken();
    }

    @Test
    @DisplayName("모든 게시글 조회")
    void getAllPosts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts").param("p", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list").isArray())
                .andExpect(jsonPath("$.list[0].tags").isArray());
    }

    @Test
    @DisplayName("태그별 게시글 조회")
    void getTagPosts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/tag")
                        .param("t", tag.getSlug())
                        .param("p", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list").isArray());
    }

    @Test
    @DisplayName("검색 결과별 게시글 조회 - HIDE 및 삭제 게시글 제외")
    void getSearchedPosts_excludesHiddenAndDeletedPosts() throws Exception {
        Post hiddenPost = taggedPost("private searchable title", "private searchable content", "private-searchable-title");
        hiddenPost.updatePostStatus(PostStatus.HIDE);

        Post deletedPost = taggedPost("deleted searchable title", "deleted searchable content", "deleted-searchable-title");
        deletedPost.delete();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/search")
                        .param("p", "1")
                        .param("type", String.valueOf(SearchType.TITLE))
                        .param("contents", "searchable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list.length()").value(0));
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 (slug) — GET은 조회수 증가 없이 순수 조회")
    void getPostDetailV1BySlug_doesNotIncrementViews() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.slug").value(postSlug))
                .andExpect(jsonPath("$.views").value(0))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(cookie().doesNotExist("viewed_posts"));
    }

    @Test
    @DisplayName("게시글 조회수 증가 — POST만 증가하고 쿠키 중복 호출은 증가하지 않음")
    void incrementPostViewsByPostOnlyDeduplicatesWithCookie() throws Exception {
        MvcResult firstResult = mockMvc.perform(post("/api/v1/posts/{postId}/views", postId))
                .andExpect(status().isOk())
                .andExpect(content().string("1"))
                .andReturn();

        Cookie viewedCookie = firstResult.getResponse().getCookie("viewed_posts");
        assertThat(viewedCookie).isNotNull();

        mockMvc.perform(post("/api/v1/posts/{postId}/views", postId).cookie(viewedCookie))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("이전 slug 조회 — 현재 공개 slug로 301 Location 응답")
    void getPostDetailByOldSlugReturns301ToCurrentSlug() throws Exception {
        PostReqDto modifiedPostDto = PostReqDto.builder()
                .title("modifiedTitle")
                .content("modifiedContent")
                .tags(List.of("Test"))
                .slug("new-public-slug")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .cookie(new Cookie(CookieName.ACCESS_TOKEN_COOKIE, accessToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(modifiedPostDto)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost:8080/posts/new-public-slug"));
    }

    @Test
    @DisplayName("게시글 상세 조회 V8 → V1 301 redirect")
    void getPostDetailV8Redirect() throws Exception {
        mockMvc.perform(get("/api/v8/posts/{postId}", postId))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/posts/" + postId));
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 - HIDE 게시글은 404")
    void getPostDetailV1HiddenPostReturns404() throws Exception {
        Post post = postRepository.findById(postId).orElseThrow();
        post.updatePostStatus(PostStatus.HIDE);

        mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 상세 조회 V1 - 삭제 게시글은 410")
    void getPostDetailV1DeletedPostReturns410() throws Exception {
        Post post = postRepository.findById(postId).orElseThrow();
        post.deletePost();

        mockMvc.perform(get("/api/v1/posts/{identifier}", postSlug))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("B005"));
    }

    @Test
    @DisplayName("게시글 등록")
    void writePost() throws Exception {
        PostReqDto postReqDto = PostReqDto.builder()
                .title("title")
                .content("content")
                .tags(List.of("Spring", "JPA"))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .cookie(new Cookie(CookieName.ACCESS_TOKEN_COOKIE, accessToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(postReqDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 수정")
    void editPost() throws Exception {
        PostReqDto modifiedPostDto = PostReqDto.builder()
                .title("modifiedTitle")
                .content("modifiedContent")
                .tags(List.of("Modified"))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .cookie(new Cookie(CookieName.ACCESS_TOKEN_COOKIE, accessToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(modifiedPostDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 삭제/복원/영구 삭제")
    void deleteAndRestorePost() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .cookie(new Cookie(CookieName.ACCESS_TOKEN_COOKIE, accessToken)))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/deleted-posts/{postId}", postId)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .cookie(new Cookie(CookieName.ACCESS_TOKEN_COOKIE, accessToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("게시글 등록 실패 - 인증 없음")
    void writePostWithoutAuth() throws Exception {
        PostReqDto postReqDto = PostReqDto.builder()
                .title("title")
                .content("content")
                .tags(List.of("Spring"))
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(postReqDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글 등록 실패 - 태그 누락")
    void writePostWithoutTags() throws Exception {
        PostReqDto postReqDto = PostReqDto.builder()
                .title("title")
                .content("content")
                .tags(List.of())
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .cookie(new Cookie(CookieName.ACCESS_TOKEN_COOKIE, accessToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(postReqDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전체 Slug 목록 조회 — VIEW 상태 게시글만 반환")
    void getAllSlugs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/slugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].slug").exists())
                .andExpect(jsonPath("$[0].updateDate").exists())
                .andExpect(header().string("Cache-Control", "public, max-age=3600"));
    }

    private Post taggedPost(String title, String content, String slug) {
        Post post = Post.builder()
                .admin(admin)
                .title(title)
                .content(content)
                .slug(slug)
                .build();
        post.replaceTags(List.of(tag));
        tag.incrementPostCount();
        return postRepository.save(post);
    }
}
