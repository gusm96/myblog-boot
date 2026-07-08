package com.moya.myblogboot.configuration;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.utils.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityConfigTest extends AbstractContainerBaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TagRepository tagRepository;

    @Test
    @DisplayName("태그 관리 API는 인증 없으면 401")
    void tagManagementApisRequireAuthentication() throws Exception {
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Redis\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/tags/{tagId}", src.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Spring Boot\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/tags/{tagId}", src.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/tags/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"srcId\":" + src.getId() + ",\"dstId\":" + dst.getId() + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("태그 관리 API는 ADMIN 권한이 아니면 403")
    void tagManagementApisRejectNonAdmin() throws Exception {
        String userToken = jwtTokenProvider.createAccessToken(1L, "ROLE_USER");

        mockMvc.perform(post("/api/v1/tags")
                        .cookie(new Cookie(ACCESS_TOKEN_COOKIE, userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Redis\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("태그 생성 API는 ADMIN 권한이면 201")
    void tagCreateAllowsAdmin() throws Exception {
        String adminToken = jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");

        mockMvc.perform(post("/api/v1/tags")
                        .cookie(new Cookie(ACCESS_TOKEN_COOKIE, adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Redis\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("태그 병합 API는 ADMIN 권한이면 200")
    void tagsMerge_admin_success() throws Exception {
        String adminToken = jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());

        mockMvc.perform(post("/api/v1/tags/merge")
                        .cookie(new Cookie(ACCESS_TOKEN_COOKIE, adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"srcId\":" + src.getId() + ",\"dstId\":" + dst.getId() + "}"))
                .andExpect(status().isOk());
    }
}
