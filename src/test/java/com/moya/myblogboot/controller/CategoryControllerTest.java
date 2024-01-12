package com.moya.myblogboot.controller;

import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.category.CategoryReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import org.junit.Before;
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
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RestDocumentationResultHandler restDocs;
    private static String accessToken;

    private static Long categoryId;

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
        memberRepository.save(member);

        MemberLoginReqDto loginReqDto = MemberLoginReqDto.builder()
                .username("testMember")
                .password("testPassword")
                .build();

        // Login 후 Token 발급
        accessToken = "bearer " + authService.memberLogin(loginReqDto).getAccess_token();

        for (int i = 0; i < 5; i++) {
            Category newCategory = Category.builder().name("test").build();
            Category saveCategory = categoryRepository.save(newCategory);
            categoryId = saveCategory.getId();
        }
    }

    @Test
    @DisplayName("카테고리 리스트 조회")
    void getCategoryList() throws Exception {
        // given
        String path = "/api/v1/categories";
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("카테고리 리스트 조회 V2")
    void getCategoryListV2() throws Exception {
        // given
        String path = "/api/v2/categories";
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("관리자용 카테고리 리스트")
    void getCategoryListForAdmin() throws Exception {
        // given
        String path = "/api/v1/categories-management";
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("카테고리 추가")
    void newCategory() throws Exception {
        // given
        String path = "/api/v1/categories";
        CategoryReqDto categoryReqDto = CategoryReqDto.builder().categoryName("newCategory").build();
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(categoryReqDto)));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("카테고리 수정")
    void editCategory() throws Exception {
        //given
        String path = "/api/v1/categories/" + categoryId;
        CategoryReqDto categoryReqDto = CategoryReqDto.builder().categoryName("Modified").build();
        //when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.put(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(categoryReqDto)));
        //then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("카테고리 삭제")
    void deleteCategory() throws Exception {
        //given
        String path = "/api/v1/categories/" + categoryId;
        //when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        //then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }
}