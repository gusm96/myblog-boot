package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostForRedis;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.repository.PostRedisRepository;
import com.moya.myblogboot.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final PostRedisRepository postRedisRepository;
    private final PostRepository postRepository;

    // Redis 캐시 조회 (miss 시 DB 조회 후 캐시 저장)
    public PostForRedis getPostFromCache(Long postId) {
        return postRedisRepository.findOne(postId)
                .orElseGet(() -> retrieveAndCache(postId));
    }

    @Async
    public void updatePost(PostForRedis postForRedis, Post post) {
        postForRedis.update(post);
        try {
            postRedisRepository.update(postForRedis);
        } catch (Exception e) {
            log.error("Redis 캐시 업데이트 실패 (postId={}): {}", postForRedis.getId(), e.getMessage());
        }
    }

    public void deletePost(PostForRedis postForRedis) {
        try {
            postRedisRepository.delete(postForRedis);
        } catch (Exception e) {
            log.error("Redis 캐시 삭제 실패 (postId={}): {}", postForRedis.getId(), e.getMessage());
        }
    }

    private PostForRedis retrieveAndCache(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.POST_NOT_FOUND));
        return postRedisRepository.save(post);
    }
}
