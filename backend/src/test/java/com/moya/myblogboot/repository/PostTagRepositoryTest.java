package com.moya.myblogboot.repository;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class PostTagRepositoryTest extends AbstractContainerBaseTest {

    @Autowired private AdminRepository adminRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private PostTagRepository postTagRepository;
    @Autowired private TagService tagService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("merge does not cascade-delete destination post tags")
    void merge_cascadeNotTriggered() {
        Admin admin = adminRepository.save(Admin.builder()
                .username("postTagAdmin")
                .password(passwordEncoder.encode("pw"))
                .build());
        Tag src = tagRepository.save(Tag.builder().name("Spring").slug("spring").build());
        Tag dst = tagRepository.save(Tag.builder().name("JPA").slug("jpa").build());
        Post post = Post.builder()
                .title("title")
                .content("content")
                .admin(admin)
                .slug("post-tag-test")
                .build();
        post.replaceTags(List.of(src, dst));
        src.incrementPostCount();
        dst.incrementPostCount();
        postRepository.save(post);

        tagService.merge(src.getId(), dst.getId());

        assertThat(tagRepository.findById(dst.getId())).isPresent();
        assertThat(postTagRepository.findAllByTagId(dst.getId())).hasSize(1);
    }
}
