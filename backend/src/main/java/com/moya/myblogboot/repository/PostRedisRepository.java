package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostForRedis;

import java.util.Optional;
import java.util.Set;

public interface PostRedisRepository {

    Set<Long> getKeys(String pattern);

    Optional<PostForRedis> findOne(Long postId);

    PostForRedis incrementViews(PostForRedis postForRedis);

    PostForRedis incrementLikes(PostForRedis post);

    PostForRedis decrementLikes(PostForRedis post);

    PostForRedis save(Post post);

    void delete(PostForRedis post);

    void update(PostForRedis post);
}
