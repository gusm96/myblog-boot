package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.dto.file.ImageFileDto;
import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostSlugHistory;
import com.moya.myblogboot.domain.post.PostStatus;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.dto.post.*;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.PostGoneException;
import com.moya.myblogboot.exception.custom.PostMovedPermanentlyException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.ImageFileRepository;
import com.moya.myblogboot.repository.PostRedisRepository;
import com.moya.myblogboot.repository.PostRepository;
import com.moya.myblogboot.repository.PostSlugHistoryRepository;
import com.moya.myblogboot.service.FileUploadService;
import com.moya.myblogboot.service.PostCacheService;
import com.moya.myblogboot.service.PostService;
import com.moya.myblogboot.service.TagService;
import com.moya.myblogboot.domain.event.PostChangeEvent;
import com.moya.myblogboot.utils.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

    private final TagService tagService;
    private final AdminRepository adminRepository;
    private final PostRepository postRepository;
    private final ImageFileRepository imageFileRepository;
    private final PostRedisRepository postRedisRepository;
    private final PostSlugHistoryRepository postSlugHistoryRepository;
    private final FileUploadService fileUploadService;
    private final PostCacheService postCacheService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int LIMIT = 8;

    @Override
    public PostListResDto retrieveAll(int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToPostListResDto(postRepository.findAll(PostStatus.VIEW, pageRequest));
    }

    @Override
    public PostListResDto retrieveAllByTag(String tagSlug, int page) {
        PageRequest pageRequest = PageRequest.of(page, LIMIT, Sort.by(Sort.Direction.DESC, "createDate"));
        return convertToPostListResDto(postRepository.findAllByTagSlug(tagSlug, pageRequest));
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
    public PostDetailResDto getPublicPostDetail(Long postId) {
        PostForRedis postForRedis = postCacheService.getPostFromCache(postId);
        assertPubliclyViewable(postForRedis);
        return PostDetailResDto.builder()
                .postForRedis(postForRedis)
                .build();
    }

    @Override
    public PostDetailResDto getPublicPostDetail(String identifier) {
        return getPublicPostDetail(resolvePublicPostId(identifier));
    }

    @Override
    public Long incrementPublicPostViews(Long postId) {
        PostForRedis postForRedis = postCacheService.getPostFromCache(postId);
        assertPubliclyViewable(postForRedis);
        return postRedisRepository.incrementViews(postForRedis).totalViews();
    }

    @Override
    public void assertPubliclyViewable(Long postId) {
        assertPubliclyViewable(postCacheService.getPostFromCache(postId));
    }

    @Override
    public Long getPublicPostViews(Long postId) {
        PostForRedis postForRedis = postCacheService.getPostFromCache(postId);
        assertPubliclyViewable(postForRedis);
        return postForRedis.totalViews();
    }

    @Override
    public Long getPublicPostLikes(Long postId) {
        PostForRedis postForRedis = postCacheService.getPostFromCache(postId);
        assertPubliclyViewable(postForRedis);
        return postForRedis.totalLikes();
    }

    @Override
    @Transactional
    public Long write(PostReqDto postReqDto, Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        List<Tag> tags = tagService.resolveOrCreate(postReqDto.getTags());
        String slug = resolveSlug(postReqDto.getSlug(), postReqDto.getTitle(), null);
        Post newPost = postReqDto.toEntity(admin, slug, resolveMetaDescription(postReqDto));
        newPost.replaceTags(tags);
        if (postReqDto.getImages() != null && !postReqDto.getImages().isEmpty()) {
            saveImageFile(postReqDto.getImages(), newPost);
        }
        Post result = postRepository.save(newPost);
        tags.forEach(Tag::incrementPostCount);
        eventPublisher.publishEvent(new PostChangeEvent(this, "CREATED", result.getId(), result.getSlug(),
                tags.stream().map(Tag::getSlug).toList()));
        return result.getId();
    }

    @Override
    @Transactional
    public Long edit(Long adminId, Long postId, PostReqDto modifiedDto) {
        Post post = findById(postId);
        verifyPostAccessAuthorization(post.getAdmin().getId(), adminId);
        List<Tag> oldTags = post.getTags();
        List<Tag> newTags = tagService.resolveOrCreate(modifiedDto.getTags());
        String oldSlug = post.getSlug();
        String slug = resolveSlug(modifiedDto.getSlug(), modifiedDto.getTitle(), post);
        post.updatePost(modifiedDto.getTitle(), modifiedDto.getContent(), slug,
                resolveMetaDescription(modifiedDto), modifiedDto.getMetaKeywords(), modifiedDto.getThumbnailUrl());
        updatePostCounts(oldTags, newTags, false);
        post.replaceTags(newTags);
        recordSlugHistoryIfChanged(post, oldSlug, slug);
        postCacheService.updatePost(postCacheService.getPostFromCache(post.getId()), post);
        eventPublisher.publishEvent(new PostChangeEvent(this, "UPDATED", postId, post.getSlug(),
                mergeTagSlugs(oldTags, newTags)));
        return postId;
    }

    @Override
    @Transactional
    public void delete(Long postId, Long adminId) {
        Post post = findById(postId);
        verifyPostAccessAuthorization(post.getAdmin().getId(), adminId);
        post.deletePost();
        post.getTags().forEach(Tag::decrementPostCount);
        postCacheService.updatePost(postCacheService.getPostFromCache(post.getId()), post);
        eventPublisher.publishEvent(new PostChangeEvent(this, "DELETED", postId, post.getSlug(),
                post.getTags().stream().map(Tag::getSlug).toList()));
    }

    @Override
    @Transactional
    public void undelete(Long postId, Long adminId) {
        Post post = findById(postId);
        verifyPostAccessAuthorization(post.getAdmin().getId(), adminId);
        post.undeletePost();
        post.getTags().forEach(Tag::incrementPostCount);
        postCacheService.updatePost(postCacheService.getPostFromCache(post.getId()), post);
        eventPublisher.publishEvent(new PostChangeEvent(this, "CREATED", postId, post.getSlug(),
                post.getTags().stream().map(Tag::getSlug).toList()));
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
    public Long getPostIdBySlug(String slug) {
        return postRepository.findIdBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND));
    }

    @Override
    public List<PostSlugDto> getAllSlugs() {
        return postRepository.findAllSlugs();
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

    private void assertPubliclyViewable(PostForRedis postForRedis) {
        if (postForRedis.getDeleteDate() != null) {
            throw new PostGoneException(ErrorCode.POST_GONE);
        }
        if (postForRedis.getPostStatus() != PostStatus.VIEW) {
            throw new EntityNotFoundException(ErrorCode.POST_NOT_FOUND);
        }
    }

    private String resolveMetaDescription(PostReqDto postReqDto) {
        if (postReqDto.getMetaDescription() == null || postReqDto.getMetaDescription().isBlank()) {
            return com.moya.myblogboot.utils.HtmlTextUtil.summarize(postReqDto.getContent(), 155);
        }
        return postReqDto.getMetaDescription();
    }

    private Long resolvePublicPostId(String identifier) {
        try {
            return Long.parseLong(identifier);
        } catch (NumberFormatException e) {
            return postRepository.findIdBySlug(identifier)
                    .orElseGet(() -> resolveMovedPostByOldSlug(identifier));
        }
    }

    private Long resolveMovedPostByOldSlug(String oldSlug) {
        PostSlugHistory history = postSlugHistoryRepository.findByOldSlug(oldSlug)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND));
        PostForRedis target = postCacheService.getPostFromCache(history.getPost().getId());
        assertPubliclyViewable(target);
        throw new PostMovedPermanentlyException(target.getSlug());
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

    private void updatePostCounts(List<Tag> oldTags, List<Tag> newTags, boolean postDeleted) {
        if (postDeleted) {
            return;
        }
        oldTags.stream()
                .filter(oldTag -> newTags.stream().noneMatch(newTag -> newTag.getId().equals(oldTag.getId())))
                .forEach(Tag::decrementPostCount);
        newTags.stream()
                .filter(newTag -> oldTags.stream().noneMatch(oldTag -> oldTag.getId().equals(newTag.getId())))
                .forEach(Tag::incrementPostCount);
    }

    private List<String> mergeTagSlugs(List<Tag> oldTags, List<Tag> newTags) {
        return java.util.stream.Stream.concat(oldTags.stream(), newTags.stream())
                .map(Tag::getSlug)
                .distinct()
                .toList();
    }

    /**
     * slug 결정 로직:
     * 1. 요청에 slug가 있으면 사용 (기존 slug와 동일하면 중복 체크 생략)
     * 2. 기존 slug가 있으면 유지 (수정 시)
     * 3. 없으면 title에서 자동 생성
     */
    private String resolveSlug(String requestedSlug, String title, Post existingPost) {
        String existingSlug = existingPost != null ? existingPost.getSlug() : null;
        Long existingPostId = existingPost != null ? existingPost.getId() : null;
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            if (requestedSlug.equals(existingSlug)) return existingSlug;
            assertSlugAvailable(requestedSlug, existingPostId);
            return requestedSlug;
        }
        if (existingSlug != null) return existingSlug;
        return generateUniqueSlug(title);
    }

    private String generateUniqueSlug(String title) {
        String base = SlugUtil.generate(title);
        if (isSlugAvailable(base, null)) return base;
        for (int i = 2; i <= 10; i++) {
            String candidate = SlugUtil.withSuffix(base, i);
            if (isSlugAvailable(candidate, null)) return candidate;
        }
        return base + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void assertSlugAvailable(String slug, Long currentPostId) {
        if (!isSlugAvailable(slug, currentPostId)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_POST_SLUG);
        }
    }

    private boolean isSlugAvailable(String slug, Long currentPostId) {
        boolean currentSlugAvailable = postRepository.findBySlug(slug)
                .map(post -> post.getId().equals(currentPostId))
                .orElse(true);
        boolean oldSlugAvailable = postSlugHistoryRepository.findByOldSlug(slug)
                .map(history -> history.getPost().getId().equals(currentPostId))
                .orElse(true);
        return currentSlugAvailable && oldSlugAvailable;
    }

    private void recordSlugHistoryIfChanged(Post post, String oldSlug, String newSlug) {
        if (oldSlug == null || oldSlug.equals(newSlug)) {
            return;
        }
        postSlugHistoryRepository.findByOldSlug(newSlug)
                .filter(history -> history.getPost().getId().equals(post.getId()))
                .ifPresent(postSlugHistoryRepository::delete);
        if (!postSlugHistoryRepository.existsByOldSlug(oldSlug)) {
            postSlugHistoryRepository.save(new PostSlugHistory(post, oldSlug));
        }
    }
}
