package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.dto.category.CategoryResDto;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.service.CategoryService;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.moya.myblogboot.exception.custom.DuplicateException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceImplTest extends AbstractContainerBaseTest {

    @Autowired private CategoryService categoryService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Admin testAdmin;
    private Category testCategory;

    @BeforeEach
    void before() {
        testAdmin = adminRepository.save(Admin.builder()
                .username("categoryTestAdmin")
                .password(passwordEncoder.encode("testPw"))
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("테스트카테고리")
                .build());
    }

    @Test
    @DisplayName("카테고리 전체 조회")
    void retrieveAll() {
        List<CategoryResDto> result = categoryService.retrieveAll();

        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(c -> c.getName().equals("테스트카테고리"))).isTrue();
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void create() {
        categoryService.create("새카테고리");

        assertThat(categoryRepository.existsByName("새카테고리")).isTrue();
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 중복된 이름")
    void create_duplicate() {
        assertThrows(DuplicateException.class,
                () -> categoryService.create("테스트카테고리"));
    }

    @Test
    @DisplayName("카테고리 수정 성공")
    void update() {
        categoryService.update(testCategory.getId(), "수정된카테고리");

        assertThat(testCategory.getName()).isEqualTo("수정된카테고리");
    }

    @Test
    @DisplayName("카테고리 조회 실패 - 존재하지 않는 ID")
    void retrieve_notFound() {
        assertThrows(EntityNotFoundException.class,
                () -> categoryService.retrieve(999L));
    }

    @Test
    @DisplayName("카테고리 삭제 성공 - 게시글 없음")
    void delete() {
        Category emptyCategory = categoryRepository.save(Category.builder()
                .name("빈카테고리")
                .build());

        categoryService.delete(emptyCategory.getId());

        assertThat(categoryRepository.findById(emptyCategory.getId())).isEmpty();
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 등록된 게시글 존재")
    void delete_withPosts() {
        Post post = postRepository.save(Post.builder()
                .title("테스트게시글")
                .content("테스트내용")
                .category(testCategory)
                .admin(testAdmin)
                .build());
        testCategory.addPost(post);

        assertThrows(com.moya.myblogboot.exception.BusinessException.class,
                () -> categoryService.delete(testCategory.getId()));
    }
}
