package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.dto.post.PostListResDto;
import com.moya.myblogboot.dto.post.PostReqDto;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.CategoryRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class PostServiceImplTest extends AbstractContainerBaseTest {

    @Autowired private PostService postService;
    @Autowired private AdminRepository adminRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Admin testAdmin;
    private Admin anotherAdmin;
    private Category testCategory;
    private Post testPost;

    @BeforeEach
    void before() {
        testAdmin = adminRepository.save(Admin.builder()
                .username("postTestAdmin")
                .password(passwordEncoder.encode("testPw"))
                .build());

        anotherAdmin = adminRepository.save(Admin.builder()
                .username("postAnotherAdmin")
                .password(passwordEncoder.encode("testPw"))
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .name("게시글테스트카테고리")
                .build());

        testPost = postRepository.save(Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(testCategory)
                .admin(testAdmin)
                .build());
    }

    @Test
    @DisplayName("게시글 목록 조회")
    void retrieveAll() {
        PostListResDto result = postService.retrieveAll(0);

        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    @DisplayName("카테고리별 게시글 목록 조회")
    void retrieveAllByCategory() {
        PostListResDto result = postService.retrieveAllByCategory(testCategory.getName(), 0);

        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void write() {
        PostReqDto reqDto = PostReqDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .category(testCategory.getId())
                .build();

        Long postId = postService.write(reqDto, testAdmin.getId());

        assertThat(postId).isNotNull();
        assertThat(postRepository.findById(postId)).isPresent();
    }

    @Test
    @DisplayName("게시글 작성 실패 - 존재하지 않는 카테고리")
    void write_invalidCategory() {
        PostReqDto reqDto = PostReqDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .category(999L)
                .build();

        assertThrows(EntityNotFoundException.class,
                () -> postService.write(reqDto, testAdmin.getId()));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void edit() {
        PostReqDto modifiedDto = PostReqDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .category(testCategory.getId())
                .build();

        Long result = postService.edit(testAdmin.getId(), testPost.getId(), modifiedDto);

        assertThat(result).isEqualTo(testPost.getId());
        assertThat(testPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(testPost.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void edit_unauthorized() {
        PostReqDto modifiedDto = PostReqDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .category(testCategory.getId())
                .build();

        assertThrows(UnauthorizedAccessException.class,
                () -> postService.edit(anotherAdmin.getId(), testPost.getId(), modifiedDto));
    }

    @Test
    @DisplayName("게시글 소프트 삭제")
    void delete() {
        postService.delete(testPost.getId(), testAdmin.getId());

        assertThat(testPost.getPostStatus()).isEqualTo(PostStatus.HIDE);
        assertThat(testPost.getDeleteDate()).isNotNull();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void delete_unauthorized() {
        assertThrows(UnauthorizedAccessException.class,
                () -> postService.delete(testPost.getId(), anotherAdmin.getId()));
    }

    @Test
    @DisplayName("삭제된 게시글 복원")
    void undelete() {
        postService.delete(testPost.getId(), testAdmin.getId());
        postService.undelete(testPost.getId(), testAdmin.getId());

        assertThat(testPost.getPostStatus()).isEqualTo(PostStatus.VIEW);
        assertThat(testPost.getDeleteDate()).isNull();
    }

    @Test
    @DisplayName("게시글 조회 실패 - 존재하지 않는 ID")
    void findById_notFound() {
        assertThrows(EntityNotFoundException.class,
                () -> postService.findById(999L));
    }
}
