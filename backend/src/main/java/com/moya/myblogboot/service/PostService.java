package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.SearchType;
import com.moya.myblogboot.dto.post.PostDetailResDto;
import com.moya.myblogboot.dto.post.PostListResDto;
import com.moya.myblogboot.dto.post.PostReqDto;

import java.time.LocalDateTime;

public interface PostService {

    PostListResDto retrieveAll(int page);

    PostListResDto retrieveAllByCategory(String categoryName, int page);

    PostListResDto retrieveAllBySearched(SearchType searchType, String searchContents, int page);

    Post findById(Long postId);

    PostDetailResDto getPostDetail(Long postId);

    PostDetailResDto getPostDetailAndIncrementViews(Long postId);

    Long getPostIdBySlug(String slug);

    PostListResDto retrieveAllDeleted(int page);

    Long write(PostReqDto postReqDto, Long memberId);

    Long edit(Long memberId, Long postId, PostReqDto postReqDto);

    void undelete(Long postId, Long memberId);

    void delete(Long postId, Long memberId);

    void deletePermanently(Long postId);

    void deletePermanently(LocalDateTime thresholdDate);
}
