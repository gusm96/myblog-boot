package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.dto.post.*;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.ImageFileRepository;
import com.moya.myblogboot.repository.PostRedisRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.service.CategoryService;
import com.moya.myblogboot.service.FileUploadService;
import com.moya.myblogboot.service.PostCacheService;
import com.moya.myblogboot.service.PostService;
import com.moya.myblogboot.utils.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final CategoryService categoryService;
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    private final ImageFileRepository imageFileRepository;
    private final PostRedisRepository postRedisRepository;
    private final FileUploadService fileUploadService;
    private final PostCacheService postCacheService;

    private static final int LIMIT = 8;

    @Override
    public PostListResDto retrieveAll(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToPostListResDto(postRepository.findAll(PostStatus.VIEW, pageRequest));
    }

    @Override
    public PostListResDto retrieveAllByCategory(String categoryName, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToPostListResDto(postRepository.findAllByCategoryName(categoryName, pageRequest));
    }

    @Override
    public PostListResDto retrieveAllBySearched(SearchType searchType, String searchContents, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToPostListResDto(postRepository.findBySearchType(pageRequest, searchType, searchContents));
    }

    @Override
    public PostListResDto retrieveAllDeleted(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "deleteDate"));
        return convertToPostListResDto(postRepository.findByDeletionStatus(pageRequest));
    }

    @Override
    public PostDetailResDto getPostDetail(Long postId) {
        return PostDetailResDto.builder()
                .postForRedis(postCacheService.getPostFromCache(postId))
                .build();
    }

    @Override
    public PostDetailResDto getPostDetailAndIncrementViews(Long postId) {
        PostForRedis postForRedis = postCacheService.getPostFromCache(postId);
        return PostDetailResDto.builder()
                .postForRedis(postRedisRepository.incrementViews(postForRedis))
                .build();
    }

    @Override
    @Transactional
    public Long write(PostReqDto postReqDto, Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        Category category = categoryService.retrieve(postReqDto.getCategory());
        String slug = resolveSlug(postReqDto.getSlug(), postReqDto.getTitle(), null);
        Post newPost = postReqDto.toEntity(category, admin, slug);
        if (postReqDto.getImages() != null && !postReqDto.getImages().isEmpty()) {
            saveImageFile(postReqDto.getImages(), newPost);
        }
        Post result = postRepository.save(newPost);
        category.addPost(result);
        return result.getId();
    }

    @Override
    @Transactional
    public Long edit(Long adminId, Long postId, PostReqDto modifiedDto) {
        Post post = findById(postId);
        verifyPostAccessAuthorization(post.getAdmin().getId(), adminId);
        Category modifiedCategory = categoryService.retrieve(modifiedDto.getCategory());
        String slug = resolveSlug(modifiedDto.getSlug(), modifiedDto.getTitle(), post.getSlug());
        post.updatePost(modifiedCategory, modifiedDto.getTitle(), modifiedDto.getContent(),
                slug, modifiedDto.getMetaDescription(), modifiedDto.getMetaKeywords(), modifiedDto.getThumbnailUrl());
        postCacheService.updatePost(postCacheService.getPostFromCache(post.getId()), post);
        return postId;
    }

    @Override
    @Transactional
    public void delete(Long postId, Long adminId) {
        Post post = findById(postId);
        verifyPostAccessAuthorization(post.getAdmin().getId(), adminId);
        post.deletePost();
        postCacheService.updatePost(postCacheService.getPostFromCache(post.getId()), post);
    }

    @Override
    @Transactional
    public void undelete(Long postId, Long adminId) {
        Post post = findById(postId);
        verifyPostAccessAuthorization(post.getAdmin().getId(), adminId);
        post.undeletePost();
        postCacheService.updatePost(postCacheService.getPostFromCache(post.getId()), post);
    }

    @Override
    @Transactional
    public void deletePermanently(LocalDateTime thresholdDate) {
        postRepository.findByDeleteDate(thresholdDate).forEach(this::deletePosts);
    }

    @Override
    @Transactional
    public void deletePermanently(Long postId) {
        deletePosts(findById(postId));
    }

    @Override
    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND));
    }

    private PostListResDto convertToPostListResDto(Page<Post> posts) {
        List<PostResDto> resultList = posts.stream()
                .map(PostResDto::of)
                .toList();
        return PostListResDto.builder()
                .list(resultList)
                .totalPage(posts.getTotalPages())
                .build();
    }

    private void verifyPostAccessAuthorization(Long postAdminId, Long adminId) {
        if (!postAdminId.equals(adminId))
            throw new UnauthorizedAccessException(ErrorCode.POST_ACCESS_DENIED);
    }

    private void deletePosts(Post post) {
        PostForRedis postForRedis = postCacheService.getPostFromCache(post.getId());
        fileUploadService.deleteFiles(post.getImageFiles());
        postCacheService.deletePost(postForRedis);
        postRepository.delete(post);
    }

    private void saveImageFile(List<ImageFileDto> images, Post post) {
        List<ImageFile> imageFiles = images.stream()
                .map(image -> imageFileRepository.save(image.toEntity(post)))
                .collect(Collectors.toList());
        imageFiles.forEach(post::addImageFile);
    }

    /**
     * slug 결정 로직:
     * 1. 요청에 slug가 있으면 사용 (기존 slug와 동일하면 중복 체크 생략)
     * 2. 기존 slug가 있으면 유지 (수정 시)
     * 3. 없으면 title에서 자동 생성
     */
    private String resolveSlug(String requestedSlug, String title, String existingSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            if (requestedSlug.equals(existingSlug)) return existingSlug;
            if (!postRepository.existsBySlug(requestedSlug)) return requestedSlug;
        }
        if (existingSlug != null) return existingSlug;
        return generateUniqueSlug(title);
    }

    private String generateUniqueSlug(String title) {
        String base = SlugUtil.generate(title);
        if (!postRepository.existsBySlug(base)) return base;
        for (int i = 2; i <= 10; i++) {
            String candidate = SlugUtil.withSuffix(base, i);
            if (!postRepository.existsBySlug(candidate)) return candidate;
        }
        return base + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
