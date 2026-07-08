package com.moya.myblogboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.constants.CookieName;
import com.moya.myblogboot.domain.event.TagChangeEvent;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.domain.tag.TagAlias;
import com.moya.myblogboot.dto.tag.TagCreateReqDto;
import com.moya.myblogboot.dto.tag.TagMergeReqDto;
import com.moya.myblogboot.dto.tag.TagRenameReqDto;
import com.moya.myblogboot.repository.TagAliasRepository;
import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.utils.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@RecordApplicationEvents
@ExtendWith({RestDocumentationExtension.class, OutputCaptureExtension.class})
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class TagControllerTest extends AbstractContainerBaseTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private RestDocumentationResultHandler restDocs;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TagRepository tagRepository;
    @Autowired private TagAliasRepository tagAliasRepository;
    @Autowired private ApplicationEvents applicationEvents;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentationContextProvider) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentationContextProvider))
                .apply(springSecurity())
                .alwaysDo(restDocs)
                .build();
        this.adminToken = jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");
    }

    @Test
    @DisplayName("create returns 201 body and public Location")
    void create_normal() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tags")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TagCreateReqDto("Spring Boot"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v2/tags/spring-boot"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Spring Boot"))
                .andExpect(jsonPath("$.slug").value("spring-boot"))
                .andReturn();

        mockMvc.perform(get(result.getResponse().getHeader(HttpHeaders.LOCATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("spring-boot"));
        assertThat(applicationEvents.stream(TagChangeEvent.class)
                .filter(event -> "CREATED".equals(event.getChangeType()))
                .count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("create rejects duplicate tag")
    void create_duplicate() throws Exception {
        tagRepository.save(Tag.builder().name("Spring").slug("spring").build());

        mockMvc.perform(post("/api/v1/tags")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TagCreateReqDto("Spring"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TG002"));
    }

    @Test
    @DisplayName("get public tag redirects aliases")
    void getPublicTag_aliasRedirect() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());
        tagAliasRepository.save(TagAlias.builder().fromSlug("java-persistence").toTag(tag).build());

        mockMvc.perform(get("/api/v2/tags/{slug}", "java-persistence"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v2/tags/jpa"));
    }

    @Test
    @DisplayName("get public tag returns 404 for unknown slug")
    void getPublicTag_notFound() throws Exception {
        mockMvc.perform(get("/api/v2/tags/{slug}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TG001"));
    }

    @Test
    @DisplayName("delete rejects inbound aliases")
    void delete_rejectsInboundAliases() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        tagAliasRepository.save(TagAlias.builder().fromSlug("old-spring").toTag(tag).build());

        mockMvc.perform(delete("/api/v1/tags/{tagId}", tag.getId()).cookie(adminCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TG010"));
    }

    @Test
    @DisplayName("delete removes unused tag")
    void delete_unusedTag() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());

        mockMvc.perform(delete("/api/v1/tags/{tagId}", tag.getId()).cookie(adminCookie()))
                .andExpect(status().isNoContent());

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    @DisplayName("merge moves aliases without affected-row mismatch warning")
    void merge_repointsAliases(CapturedOutput output) throws Exception {
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());
        tagAliasRepository.save(TagAlias.builder().fromSlug("old-spring").toTag(src).build());

        mockMvc.perform(post("/api/v1/tags/merge")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TagMergeReqDto(src.getId(), dst.getId()))))
                .andExpect(status().isOk());

        assertThat(tagAliasRepository.findByFromSlug("old-spring").orElseThrow().getToTag().getId())
                .isEqualTo(dst.getId());
        assertThat(output.getOut()).doesNotContain("Tag alias repoint affected row mismatch");
    }

    @Test
    @DisplayName("merge rejects same source and destination")
    void merge_sameTarget() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());

        mockMvc.perform(post("/api/v1/tags/merge")
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TagMergeReqDto(tag.getId(), tag.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TG007"));
    }

    @Test
    @DisplayName("rename changes name only")
    void rename_normal() throws Exception {
        Tag tag = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());

        mockMvc.perform(put("/api/v1/tags/{tagId}", tag.getId())
                        .cookie(adminCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TagRenameReqDto("Spring Boot"))))
                .andExpect(status().isOk());

        Tag renamed = tagRepository.findById(tag.getId()).orElseThrow();
        assertThat(renamed.getName()).isEqualTo("Spring Boot");
        assertThat(renamed.getSlug()).isEqualTo("spring");
    }

    private Cookie adminCookie() {
        return new Cookie(CookieName.ACCESS_TOKEN_COOKIE, adminToken);
    }
}
