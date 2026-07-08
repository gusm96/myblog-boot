package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.event.PostChangeEvent;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.dto.post.PostListResDto;
import com.moya.myblogboot.dto.post.PostReqDto;
import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.PostTagRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class PostServiceImplTest extends AbstractContainerBaseTest {

    @Autowired private PostService postService;
    @Autowired private AdminRepository adminRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private PostTagRepository postTagRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ApplicationEvents applicationEvents;

    private Admin testAdmin;
    private Admin anotherAdmin;
    private Tag testTag;
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

        testTag = tagRepository.save(Tag.builder()
                .name("게시글테스트태그")
                .slug("post-test-tag")
                .build());

        testPost = Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .admin(testAdmin)
                .build();
        testPost.replaceTags(List.of(testTag));
        testTag.incrementPostCount();
        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("게시글 목록 조회")
    void retrieveAll() {
        PostListResDto result = postService.retrieveAll(0);

        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    @DisplayName("태그별 게시글 목록 조회")
    void retrieveAllByTag() {
        PostListResDto result = postService.retrieveAllByTag(testTag.getSlug(), 0);

        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void write() {
        PostReqDto reqDto = PostReqDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .tags(List.of("Spring", "JPA"))
                .build();

        Long postId = postService.write(reqDto, testAdmin.getId());

        Post savedPost = postRepository.findById(postId).orElseThrow();
        assertThat(savedPost.getTags()).extracting(Tag::getSlug).containsExactly("spring", "jpa");
    }

    @Test
    @DisplayName("write assigns tag sort order in input order")
    void write_assignsSortOrderInInputOrder() {
        PostReqDto reqDto = PostReqDto.builder()
                .title("ordered tags")
                .content("content")
                .tags(List.of("Alpha", "Beta", "Gamma"))
                .build();

        Long postId = postService.write(reqDto, testAdmin.getId());

        Post savedPost = postRepository.findById(postId).orElseThrow();
        assertThat(savedPost.getPostTags()).extracting("sortOrder").containsExactly(0, 1, 2);
        assertThat(savedPost.getTags()).extracting(Tag::getSlug).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    @DisplayName("게시글 작성 - metaDescription이 blank면 HTML 본문에서 자동 생성")
    void write_generatesMetaDescriptionFromContentWhenBlank() {
        PostReqDto reqDto = PostReqDto.builder()
                .title("새 게시글")
                .content("<p>Hello <strong>SEO</strong> &amp; blog</p><script>alert('x')</script>")
                .tags(List.of("SEO"))
                .metaDescription(" ")
                .build();

        Long postId = postService.write(reqDto, testAdmin.getId());

        Post savedPost = postRepository.findById(postId).orElseThrow();
        assertThat(savedPost.getMetaDescription()).isEqualTo("Hello SEO & blog");
    }

    @Test
    @DisplayName("게시글 수정 - metaDescription이 blank면 HTML 본문에서 자동 생성")
    void edit_generatesMetaDescriptionFromContentWhenBlank() {
        PostReqDto modifiedDto = PostReqDto.builder()
                .title("수정된 제목")
                .content("<h1>Updated</h1><p>description text</p>")
                .tags(List.of(testTag.getName()))
                .metaDescription("")
                .build();

        postService.edit(testAdmin.getId(), testPost.getId(), modifiedDto);

        assertThat(testPost.getMetaDescription()).isEqualTo("Updated description text");
    }

    @Test
    @DisplayName("게시글 작성 실패 - 태그 없음")
    void write_withoutTags() {
        PostReqDto reqDto = PostReqDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .tags(List.of())
                .build();

        assertThrows(BusinessException.class,
                () -> postService.write(reqDto, testAdmin.getId()));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void edit() {
        PostReqDto modifiedDto = PostReqDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .tags(List.of("수정태그"))
                .build();

        Long result = postService.edit(testAdmin.getId(), testPost.getId(), modifiedDto);

        assertThat(result).isEqualTo(testPost.getId());
        assertThat(testPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(testPost.getContent()).isEqualTo("수정된 내용");
        assertThat(testPost.getTags()).extracting(Tag::getName).containsExactly("수정태그");
    }

    @Test
    @DisplayName("edit increments and decrements changed tags only")
    void edit_diffIncrementsDecrements() {
        Long postId = postService.write(PostReqDto.builder()
                .title("tag count post")
                .content("content")
                .tags(List.of("Alpha", "Beta"))
                .build(), testAdmin.getId());
        Tag alpha = tagRepository.findBySlug("alpha").orElseThrow();
        Tag beta = tagRepository.findBySlug("beta").orElseThrow();

        postService.edit(testAdmin.getId(), postId, PostReqDto.builder()
                .title("tag count post edited")
                .content("content")
                .tags(List.of("Beta", "Gamma"))
                .build());

        Tag gamma = tagRepository.findBySlug("gamma").orElseThrow();
        assertThat(alpha.getPostCount()).isZero();
        assertThat(beta.getPostCount()).isOne();
        assertThat(gamma.getPostCount()).isOne();
    }

    @Test
    @DisplayName("edit keeps overlapping tag without unique constraint violation")
    void edit_keepsExistingTagWithoutUniqueViolation() {
        Long postId = postService.write(PostReqDto.builder()
                .title("overlap post")
                .content("content")
                .tags(List.of("Alpha", "Beta"))
                .build(), testAdmin.getId());

        assertThatCode(() -> postService.edit(testAdmin.getId(), postId, PostReqDto.builder()
                .title("overlap post edited")
                .content("content")
                .tags(List.of("Alpha", "Gamma"))
                .build())).doesNotThrowAnyException();

        Post editedPost = postRepository.findById(postId).orElseThrow();
        assertThat(editedPost.getPostTags()).hasSize(2);
        assertThat(editedPost.getTags()).extracting(Tag::getSlug).containsExactly("alpha", "gamma");
    }

    @Test
    @DisplayName("게시글 수정 실패 - 권한 없음")
    void edit_unauthorized() {
        PostReqDto modifiedDto = PostReqDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .tags(List.of(testTag.getName()))
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
        assertThat(testTag.getPostCount()).isZero();
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
        assertThat(testTag.getPostCount()).isOne();
    }

    @Test
    @DisplayName("undelete publishes CREATED event with tag slugs")
    void undelete_incrementsAndPublishesCreatedEvent() {
        postService.delete(testPost.getId(), testAdmin.getId());

        postService.undelete(testPost.getId(), testAdmin.getId());

        assertThat(applicationEvents.stream(PostChangeEvent.class)
                .filter(event -> "CREATED".equals(event.getChangeType()))
                .filter(event -> testPost.getId().equals(event.getPostId()))
                .filter(event -> event.getTagSlugs().contains(testTag.getSlug()))
                .count()).isOne();
    }

    @Test
    @DisplayName("soft delete decrements count but preserves post tags")
    void softDelete_decrementsButPreservesPostTag() {
        postService.delete(testPost.getId(), testAdmin.getId());

        assertThat(testTag.getPostCount()).isZero();
        assertThat(postTagRepository.findAllByTagId(testTag.getId())).hasSize(1);
    }

    @Test
    @DisplayName("게시글 조회 실패 - 존재하지 않는 ID")
    void findById_notFound() {
        assertThrows(EntityNotFoundException.class,
                () -> postService.findById(999L));
    }
}
