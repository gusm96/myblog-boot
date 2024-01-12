package com.moya.myblogboot.controller;

import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.MemberLoginReqDto;
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
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;


@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RestDocsConfiguration.class)
@ExtendWith(RestDocumentationExtension.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestDocumentationResultHandler restDocs;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    private static String accessToken;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentationContextProvider) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentationContextProvider))
                .apply(springSecurity())
                .alwaysDo(restDocs)
                .build();
    }

    @BeforeEach
    void login() {
        // 관리자 회원 생성
        Member admin = Member.builder()
                .username("adminUser")
                .password(passwordEncoder.encode("adminPassword"))
                .nickname("Admin")
                .build();
        admin.addRoleAdmin();
        Member saveAdmin = memberRepository.save(admin);

        // 관리자 회원 로그인
        MemberLoginReqDto loginReqDto = MemberLoginReqDto.builder()
                .username("adminUser")
                .password("adminPassword")
                .build();
        accessToken = "bearer " + authService.memberLogin(loginReqDto).getAccess_token();
    }

    @Test
    @DisplayName("S3 이미지 업로드")
    void uploadImageFile() throws Exception {
        // given
        String path = "/api/v1/images";
        MockMultipartFile multipartFile = new MockMultipartFile(
                "image", "test-image.jpg", "image/jpeg", "MockMvc Test Content".getBytes()
        );
        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, path)
                .file(multipartFile)
                .header(HttpHeaders.AUTHORIZATION, accessToken));
        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    void deleteImageFile() throws Exception {
        // given
        String path = "/api/v1/images";
        // 이미지 업로드 먼저.
        MockMultipartFile multipartFile = new MockMultipartFile(
                "image", "test-image.jpg", "image/jpeg", "MockMvc Test Content".getBytes()
        );
        String contentAsString = mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, path)
                        .file(multipartFile)
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        ImageFileDto imageFileDto = objectMapper.readValue(contentAsString, ImageFileDto.class);

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(imageFileDto)));

        // then
        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }
}