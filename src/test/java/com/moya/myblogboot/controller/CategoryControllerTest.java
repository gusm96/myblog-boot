package com.moya.myblogboot.controller;

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
class CategoryControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    AuthService authService;

    @Autowired
    CategoryRepository categoryRepository;
    private static String accessToken;

    private static Long categoryId;

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
    void deleteCategory()throws Exception {
        //given
        String path = "/api/v1/categories/" + categoryId;
        //when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        //then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }
}