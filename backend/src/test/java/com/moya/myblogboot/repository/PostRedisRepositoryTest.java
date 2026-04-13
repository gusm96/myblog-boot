package com.moya.myblogboot.repository;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostForRedis;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class PostRedisRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    AdminRepository adminRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static Long postId;

    @BeforeEach
    void before() {
        Admin admin = Admin.builder()
                .username("testAdmin")
                .password("testPassword")
                .build();
        Admin savedAdmin = adminRepository.save(admin);

        Category newCategory = Category.builder().name("Category").build();
        Category category = categoryRepository.save(newCategory);

        Post newPost = Post.builder()
                .title("제목")
                .content("내용")
                .category(category)
                .admin(savedAdmin)
                .build();
        Post post = postRepository.save(newPost);
        postId = post.getId();
    }

    @Test
    @DisplayName("게시글 조회 v3")
    void 게시글_조회_V3() {
        Post findPost = postRepository.findById(postId).orElseThrow(
                () -> new EntityNotFoundException("게시글이 존재하지 않습니다.")
        );
        PostForRedis postDto = PostForRedis.builder()
                .post(findPost)
                .build();

        String key = "post:" + findPost.getId();
        redisTemplate.opsForValue().set(key, postDto);
        PostForRedis redisPost = (PostForRedis) redisTemplate.opsForValue().get(key);
        redisPost.incrementViews();
        redisTemplate.opsForValue().set(key, redisPost);

        assertThat(redisPost.getId()).isEqualTo(findPost.getId());
    }
}
