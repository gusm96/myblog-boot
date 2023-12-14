package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.service.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void before() {
        Member member = Member.builder()
                .username("testUser")
                .password(passwordEncoder.encode("testPassword"))
                .nickname("testNickname")
                .build();
        em.persist(member);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("회원 가입 테스트")
    void join() throws Exception {
        // given
        MemberJoinReqDto requestBody = MemberJoinReqDto.builder()
                .username("testUser1")
                .password("testPassword1")
                .nickname("tester1")
                .build();
        // when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/join")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(requestBody))
                )
                //then
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("회원가입을 성공했습니다."));
    }

    @Test
    @DisplayName("로그인 테스트")
    void login() throws Exception {
        //given
        MemberLoginReqDto requestBody = getTestMemberDto();

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                        .contentType("application/json").content(new ObjectMapper().writeValueAsString(requestBody)))
                //then
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.cookie().exists("refresh_token"));
    }

    @Test
    @DisplayName("로그아웃 테스트")
    void logout() throws Exception {
        // given
        // 로그인 후 토큰 생성
        String refreshToken = getToken().getRefresh_token();

        // when
        // 로그 아웃
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/logout")
                        .cookie(new Cookie("refresh_token", refreshToken))) // 임시로 쿠키값 삽입
                //then
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.cookie().value("refresh_token", Matchers.nullValue()));
    }

    @Test
    @DisplayName("토큰 권한 확인 테스트")
    void getTokenFromRole() throws Exception {
        // given
        // 로그인 후 토큰 생성
        String accessToken  = "bearer " + getToken().getAccess_token();

        //when
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-role")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                // then
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Access Token 재발급 테스트")
    void reissuingAccessToken() throws Exception {
        // given
        String refreshToken = getToken().getRefresh_token();
        // when
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reissuing-token")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                // then
                .andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    @DisplayName("토큰 만료 확인 테스트")
    void tokenValidate()throws Exception {
        // given
        // Access Token
        String accessToken = "bearer " + getToken().getAccess_token();
        // when
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-validation")
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                // then
                .andExpect(MockMvcResultMatchers.status().isOk());
    }


    private static MemberLoginReqDto getTestMemberDto() {
        MemberLoginReqDto requestBody = MemberLoginReqDto.builder()
                .username("testUser")
                .password("testPassword")
                .build();
        return requestBody;
    }
    private Token getToken() {
        return authService.memberLogin(getTestMemberDto());
    }
}