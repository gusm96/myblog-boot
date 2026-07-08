package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.RedisTestCleaner;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;
import static com.moya.myblogboot.constants.CookieName.REFRESH_TOKEN_COOKIE;
import static com.moya.myblogboot.support.TokenTestSupport.rawRefreshToken;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({RestDocumentationExtension.class, OutputCaptureExtension.class})
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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        RedisTestCleaner.deleteLoginAttemptKeys(stringRedisTemplate);
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
        LoginReqDto requestBody = getTestAdminDto();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(requestBody)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.cookie().exists(ACCESS_TOKEN_COOKIE))
                .andExpect(MockMvcResultMatchers.cookie().httpOnly(ACCESS_TOKEN_COOKIE, true))
                .andExpect(MockMvcResultMatchers.cookie().exists(REFRESH_TOKEN_COOKIE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tokenType").value("Bearer"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.expiresIn").value(600))
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("username").description("관리자 아이디"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.SET_COOKIE).description("Access/Refresh Token (HttpOnly Cookie)")
                        ),
                        responseFields(
                                fieldWithPath("tokenType").description("토큰 타입"),
                                fieldWithPath("expiresIn").description("Access Token 만료까지 남은 초")
                        )
                ));
    }

    @Test
    @DisplayName("로그아웃 테스트")
    void logout() throws Exception {
        IssuedToken token = getToken();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/logout")
                .cookie(new Cookie(REFRESH_TOKEN_COOKIE, token.refreshToken()))
                .cookie(new Cookie(ACCESS_TOKEN_COOKIE, token.accessToken())));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.cookie().maxAge(REFRESH_TOKEN_COOKIE, 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge(ACCESS_TOKEN_COOKIE, 0));
    }

    @Test
    @DisplayName("토큰 권한 확인 테스트")
    void getTokenFromRole() throws Exception {
        String accessToken = getToken().accessToken();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-role")
                .cookie(new Cookie(ACCESS_TOKEN_COOKIE, accessToken)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("ADMIN"))
                .andDo(restDocs.document(
                        responseBody()
                ));
    }

    @Test
    @DisplayName("Access Token 재발급 테스트")
    void reissuingAccessToken() throws Exception {
        String refreshToken = getToken().refreshToken();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/reissuing-token")
                .cookie(new Cookie(REFRESH_TOKEN_COOKIE, refreshToken)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.cookie().exists(ACCESS_TOKEN_COOKIE))
                .andExpect(MockMvcResultMatchers.cookie().exists(REFRESH_TOKEN_COOKIE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tokenType").value("Bearer"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.expiresIn").value(600))
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("tokenType").description("토큰 타입"),
                                fieldWithPath("expiresIn").description("Access Token 만료까지 남은 초")
                        )
                ));
    }

    @Test
    @DisplayName("토큰 권한 확인 - 토큰 없음 또는 잘못된 토큰은 401")
    void tokenRoleWithoutValidCookie() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-role"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/token-role")
                        .cookie(new Cookie(ACCESS_TOKEN_COOKIE, "invalid-token")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.cookie().maxAge(ACCESS_TOKEN_COOKIE, 0));
    }

    @Test
    @DisplayName("Authorization 헤더만으로 보호 API를 호출하면 401")
    void authorizationHeaderOnlyCannotAccessProtectedApi() throws Exception {
        String accessToken = "Bearer " + getToken().accessToken();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/tags")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType("application/json")
                        .content("{\"name\":\"new-tag\"}"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("GET 로그아웃과 GET 재발급은 405")
    void getLogoutAndGetReissuingTokenAreMethodNotAllowed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/logout"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reissuing-token"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("토큰 재발급 실패 시 auth 쿠키 전체를 삭제한다")
    void reissuingTokenFailureDeletesAuthCookies() throws Exception {
        String expiredRefreshToken = rawRefreshToken(-60_000L);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/reissuing-token")
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE, expiredRefreshToken))
                        .cookie(new Cookie(ACCESS_TOKEN_COOKIE, "stale-access-token")))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.cookie().maxAge(REFRESH_TOKEN_COOKIE, 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge(ACCESS_TOKEN_COOKIE, 0));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void loginWithWrongPassword(CapturedOutput output) throws Exception {
        LoginReqDto requestBody = LoginReqDto.builder()
                .username("testuser")
                .password("wrongPassword")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        String logs = output.getOut();
        assertTrue(logs.contains("event=LOGIN_FAILURE"));
        assertTrue(logs.contains("result=FAILURE"));
        assertTrue(logs.contains("username=testuser"));
        assertTrue(logs.contains("ip="));
        assertTrue(logs.contains("ua="));
        assertFalse(logs.contains("wrongPassword"));
    }

    @Test
    @DisplayName("로그인 실패 5회째부터 429와 Retry-After를 반환한다")
    void loginBruteForceProtection(CapturedOutput output) throws Exception {
        LoginReqDto requestBody = LoginReqDto.builder()
                .username("testuser")
                .password("wrongPassword")
                .build();

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("A007"))
                    .andExpect(MockMvcResultMatchers.header().doesNotExist(HttpHeaders.RETRY_AFTER));
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(MockMvcResultMatchers.status().isTooManyRequests())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("A009"))
                .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER));

        String logs = output.getOut();
        assertTrue(logs.contains("event=LOGIN_LOCKED"));
        assertFalse(logs.contains("event=LOGIN_FAILURE result=FAILURE username=testuser")
                && logs.lastIndexOf("event=LOGIN_FAILURE result=FAILURE username=testuser")
                > logs.lastIndexOf("event=LOGIN_LOCKED"));
        assertFalse(logs.contains("wrongPassword"));
    }

    @Test
    @DisplayName("로그인 검증 실패 응답은 민감한 rejectedValue를 마스킹한다")
    void loginValidationMasksSensitiveRejectedValue() throws Exception {
        String rawPassword = "too-long-password-value";
        LoginReqDto requestBody = LoginReqDto.builder()
                .username("testuser")
                .password(rawPassword)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string(containsString("[PROTECTED]")))
                .andExpect(MockMvcResultMatchers.content().string(not(containsString(rawPassword))));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 쿠키 없음")
    void reissuingTokenWithNoCookie() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/reissuing-token"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.cookie().maxAge(REFRESH_TOKEN_COOKIE, 0))
                .andExpect(MockMvcResultMatchers.cookie().maxAge(ACCESS_TOKEN_COOKIE, 0));
    }

    private static LoginReqDto getTestAdminDto() {
        return LoginReqDto.builder()
                .username("testuser")
                .password("testPassword")
                .build();
    }

    private IssuedToken getToken() {
        return authService.adminLogin(getTestAdminDto());
    }
}
