package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.dto.post.PostForRedis;
import com.moya.myblogboot.repository.PostRedisRepository;
import com.moya.myblogboot.service.PostCacheService;
import com.moya.myblogboot.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl implements PostLikeService {

    private final PostCacheService postCacheService;
    private final PostRedisRepository postRedisRepository;

    @Override
    public Long addLikes(Long postId) {
        PostForRedis post = postCacheService.getPostFromCache(postId);
        return postRedisRepository.incrementLikes(post).totalLikes();
    }

    @Override
    public Long cancelLikes(Long postId) {
        PostForRedis post = postCacheService.getPostFromCache(postId);
        return postRedisRepository.decrementLikes(post).totalLikes();
    }
}
