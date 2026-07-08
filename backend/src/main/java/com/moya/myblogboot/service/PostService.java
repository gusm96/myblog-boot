package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.dto.post.PostDetailResDto;
import com.moya.myblogboot.dto.post.PostListResDto;
import com.moya.myblogboot.dto.post.PostReqDto;
import com.moya.myblogboot.dto.post.PostSlugDto;

import java.time.LocalDateTime;
import java.util.List;

public interface PostService {

    PostListResDto retrieveAll(int page);

    PostListResDto retrieveAllByTag(String tagSlug, int page);

    PostListResDto retrieveAllBySearched(SearchType searchType, String searchContents, int page);

    Post findById(Long postId);

    PostDetailResDto getPostDetail(Long postId);

    PostDetailResDto getPublicPostDetail(Long postId);

    PostDetailResDto getPublicPostDetail(String identifier);

    Long incrementPublicPostViews(Long postId);

    void assertPubliclyViewable(Long postId);

    Long getPublicPostViews(Long postId);

    Long getPublicPostLikes(Long postId);

    Long getPostIdBySlug(String slug);

    List<PostSlugDto> getAllSlugs();

    PostListResDto retrieveAllDeleted(int page);

    Long write(PostReqDto postReqDto, Long memberId);

    Long edit(Long memberId, Long postId, PostReqDto postReqDto);

    void undelete(Long postId, Long memberId);

    void delete(Long postId, Long memberId);

    void deletePermanently(Long postId);

    void deletePermanently(LocalDateTime thresholdDate);
}
