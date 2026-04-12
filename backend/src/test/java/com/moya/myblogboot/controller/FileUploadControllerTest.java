package com.moya.myblogboot.controller;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.dto.file.ImageFileDto;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RestDocsConfiguration.class)
@ExtendWith(RestDocumentationExtension.class)
class FileUploadControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestDocumentationResultHandler restDocs;
    @Autowired
    private AuthService authService;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AmazonS3 amazonS3;

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
    void login() {
        Admin admin = Admin.builder()
                .username("adminUser")
                .password(passwordEncoder.encode("adminPassword"))
                .build();
        adminRepository.save(admin);

        LoginReqDto loginReqDto = LoginReqDto.builder()
                .username("adminUser")
                .password("adminPassword")
                .build();
        accessToken = "bearer " + authService.adminLogin(loginReqDto).getAccess_token();
    }

    @BeforeEach
    void mockS3() throws Exception {
        given(amazonS3.putObject(any(PutObjectRequest.class))).willReturn(new PutObjectResult());
        given(amazonS3.getUrl(anyString(), anyString()))
                .willReturn(new URL("https://s3.ap-northeast-2.amazonaws.com/myblog-boot-bucket/test-image.jpg"));
        doNothing().when(amazonS3).deleteObject(anyString(), anyString());
    }

    @Test
    @DisplayName("S3 이미지 업로드")
    void uploadImageFile() throws Exception {
        String path = "/api/v1/images";
        MockMultipartFile multipartFile = new MockMultipartFile(
                "image", "test-image.jpg", "image/jpeg", "MockMvc Test Content".getBytes()
        );

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, path)
                .file(multipartFile)
                .header(HttpHeaders.AUTHORIZATION, accessToken));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        requestParts(
                                partWithName("image").description("업로드할 이미지 파일")
                        ),
                        responseFields(
                                fieldWithPath("fileName").description("S3에 저장된 파일명 (UUID + 원본파일명)"),
                                fieldWithPath("filePath").description("S3 파일 접근 URL")
                        )
                ));
    }

    @Test
    @DisplayName("S3 이미지 삭제")
    void deleteImageFile() throws Exception {
        String path = "/api/v1/images";
        ImageFileDto imageFileDto = ImageFileDto.builder()
                .fileName("test-uuid-test-image.jpg")
                .filePath("https://s3.ap-northeast-2.amazonaws.com/myblog-boot-bucket/test-uuid-test-image.jpg")
                .build();

        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(path)
                .header(HttpHeaders.AUTHORIZATION, accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(imageFileDto)));

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(restDocs.document(
                        requestHeaders(
                                headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer Access Token (관리자)")
                        ),
                        requestFields(
                                fieldWithPath("fileName").description("삭제할 S3 파일명"),
                                fieldWithPath("filePath").description("삭제할 S3 파일 URL")
                        )
                ));
    }
}
