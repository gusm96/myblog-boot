package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.AuthService;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class AuthControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager em;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private RestDocumentationResultHandler restDocs;
    @Autowired
    private ObjectMapper objectMapper;

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
                .username("testuser")
                .password(passwordEncoder.encode("testPassword"))
                .build();
        adminRepository.save(admin);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("로그인 테스트")
    void login() throws Exception {
        MemberLoginReqDto requestBody = getTestAdminDto();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestBody)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("username").description("관리자 아이디"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.SET_COOKIE).description("Refresh Token (HttpOnly Cookie)")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("로그아웃 테스트")
    void logout() throws Exception {
        String refreshToken = getToken().getRefresh_token();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/logout")
                .cookie(new Cookie("refresh_token", refreshToken)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("토큰 권한 확인 테스트")
    void getTokenFromRole() throws Exception {
        String accessToken = "bearer " + getToken().getAccess_token();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-role")
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("Access Token 재발급 테스트")
    void reissuingAccessToken() throws Exception {
        String refreshToken = getToken().getRefresh_token();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reissuing-token")
                .cookie(new Cookie("refresh_token", refreshToken)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        responseBody()
                ));
    }

    @Test
    @DisplayName("토큰 만료 확인 테스트")
    void tokenValidate() throws Exception {
        String accessToken = "bearer " + getToken().getAccess_token();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-validation")
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token")
                        ),
                        responseBody()
                ));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void loginWithWrongPassword() throws Exception {
        MemberLoginReqDto requestBody = MemberLoginReqDto.builder()
                .username("testuser")
                .password("wrongPassword")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 쿠키 없음")
    void reissuingTokenWithNoCookie() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reissuing-token"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private static MemberLoginReqDto getTestAdminDto() {
        return MemberLoginReqDto.builder()
                .username("testuser")
                .password("testPassword")
                .build();
    }

    private Token getToken() {
        return authService.adminLogin(getTestAdminDto());
    }
}
