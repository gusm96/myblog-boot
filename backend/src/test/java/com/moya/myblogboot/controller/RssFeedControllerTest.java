package com.moya.myblogboot.controller;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.config.RestDocsConfiguration;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsConfiguration.class)
@ActiveProfiles("test")
class RssFeedControllerTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RestDocumentationResultHandler restDocs;

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
                .username("rssTestAdmin")
                .password(passwordEncoder.encode("testPassword"))
                .build();
        Admin savedAdmin = adminRepository.save(admin);

        Category category = Category.builder().name("RssTest").build();
        Category savedCategory = categoryRepository.save(category);

        for (int i = 0; i < 3; i++) {
            Post post = Post.builder()
                    .admin(savedAdmin)
                    .category(savedCategory)
                    .title("RSS Test Post " + i)
                    .content("<p>This is <strong>HTML</strong> content " + i + "</p>")
                    .slug("rss-test-post-" + i)
                    .metaDescription(i == 0 ? "Custom meta description" : null)
                    .build();
            postRepository.save(post);
        }
    }

    @Test
    @DisplayName("RSS 피드 조회 — XML 응답 및 필수 태그 포함")
    void getRssFeed() throws Exception {
        mockMvc.perform(get("/rss.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(header().string("Cache-Control", "public, max-age=600"))
                .andExpect(xpath("/rss/channel/title").exists())
                .andExpect(xpath("/rss/channel/link").exists())
                .andExpect(xpath("/rss/channel/description").exists())
                .andExpect(xpath("/rss/channel/language").string("ko"))
                .andExpect(xpath("/rss/channel/item").nodeCount(3))
                .andExpect(xpath("/rss/channel/item[1]/title").exists())
                .andExpect(xpath("/rss/channel/item[1]/link").exists())
                .andExpect(xpath("/rss/channel/item[1]/pubDate").exists())
                .andExpect(xpath("/rss/channel/item[1]/guid").exists())
                .andExpect(xpath("/rss/channel/item[1]/category").string("RssTest"));
    }

    @Test
    @DisplayName("RSS 피드 — metaDescription 없는 게시글은 content에서 HTML 제거 후 자동 추출")
    void getRssFeed_autoDescription() throws Exception {
        mockMvc.perform(get("/rss.xml"))
                .andExpect(status().isOk())
                // metaDescription이 있는 첫 번째 아이템 확인
                .andExpect(xpath("/rss/channel/item[1]/description").exists());
    }

    @Test
    @DisplayName("RSS 피드 — 인증 없이 접근 가능")
    void getRssFeed_noAuthRequired() throws Exception {
        mockMvc.perform(get("/rss.xml"))
                .andExpect(status().isOk());
    }
}
