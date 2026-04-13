package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.dto.category.CategoryReqDto;
import com.moya.myblogboot.dto.auth.LoginReqDto;
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
class CategoryControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private RestDocumentationResultHandler restDocs;
    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long categoryId;

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

        LoginReqDto loginReqDto = LoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        accessToken = "bearer " + authService.adminLogin(loginReqDto).getAccess_token();

        for (int i = 0; i < 5; i++) {
            Category newCategory = Category.builder().name("test" + i).build();
            Category saveCategory = categoryRepository.save(newCategory);
            categoryId = saveCategory.getId();

            // getCategoryListV2는 VIEW 게시글이 있는 카테고리만 반환하므로 게시글 생성
            Post post = Post.builder()
                    .admin(saveAdmin)
                    .category(saveCategory)
                    .title("title" + i)
                    .content("content" + i)
                    .build();
            postRepository.save(post);
        }
    }

    @Test
    @DisplayName("카테고리 리스트 조회")
    void getCategoryList() throws Exception {
        String path = "/api/v1/categories";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("[].id").description("카테고리 ID"),
                                fieldWithPath("[].name").description("카테고리명")
                        )
                ));
    }

    @Test
    @DisplayName("카테고리 리스트 조회 V2")
    void getCategoryListV2() throws Exception {
        String path = "/api/v2/categories";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("[].id").description("카테고리 ID"),
                                fieldWithPath("[].name").description("카테고리명"),
                                fieldWithPath("[].postsCount").description("카테고리 내 게시글 수")
                        )
                ));
    }

    @Test
    @DisplayName("관리자용 카테고리 리스트")
    void getCategoryListForAdmin() throws Exception {
        String path = "/api/v1/categories-management";

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("카테고리 ID"),
                                fieldWithPath("[].name").description("카테고리명"),
                                fieldWithPath("[].postsCount").description("카테고리 내 게시글 수")
                        )
                ));
    }

    @Test
    @DisplayName("카테고리 추가")
    void newCategory() throws Exception {
        String path = "/api/v1/categories";
        CategoryReqDto categoryReqDto = CategoryReqDto.builder().categoryName("newCategory").build();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(categoryReqDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        requestFields(
                                fieldWithPath("categoryName").description("생성할 카테고리명")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("카테고리 수정")
    void editCategory() throws Exception {
        CategoryReqDto categoryReqDto = CategoryReqDto.builder().categoryName("Modified").build();

        ResultActions resultActions = mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(categoryReqDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("categoryId").description("수정할 카테고리 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        requestFields(
                                fieldWithPath("categoryName").description("수정할 카테고리명")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("카테고리 추가 실패 - 중복 이름")
    void newCategoryWithDuplicateName() throws Exception {
        CategoryReqDto categoryReqDto = CategoryReqDto.builder().categoryName("test0").build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(categoryReqDto)))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    @DisplayName("카테고리 수정 실패 - 존재하지 않는 카테고리")
    void editCategoryNotFound() throws Exception {
        CategoryReqDto categoryReqDto = CategoryReqDto.builder().categoryName("Modified").build();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/categories/" + Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(categoryReqDto)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 인증 없음")
    void deleteCategoryWithoutAuth() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/categories/" + categoryId))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @DisplayName("카테고리 삭제")
    void deleteCategory() throws Exception {
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("categoryId").description("삭제할 카테고리 ID")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        responseBody()
                ));
    }
}
