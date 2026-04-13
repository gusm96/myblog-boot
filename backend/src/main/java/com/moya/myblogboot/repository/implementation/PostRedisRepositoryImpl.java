package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostForRedis;
import com.moya.myblogboot.repository.PostRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.moya.myblogboot.domain.keys.RedisKey.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostRedisRepositoryImpl implements PostRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long CACHE_TTL_SECONDS = 60 * 60L; // 1시간

    @Override
    public Set<Long> getKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<Long>>) connection -> {
            Set<Long> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    keys.add(Long.parseLong(key.split(":")[1]));
                }
            }
            return keys;
        });
    }

    @Override
    public Optional<PostForRedis> findOne(Long postId) {
        String key = getKey(postId);
        PostForRedis postForRedis = getPostForRedis(key);
        if (postForRedis == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(postForRedis);
        }
    }

    @Override
    public PostForRedis incrementViews(PostForRedis post) {
        String key = getKey(post.getId());
        String viewsKey = getViewsKey(key);
        Long updateViews = redisTemplate.opsForValue().increment(viewsKey);
        redisTemplate.expire(viewsKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        post.setUpdateViews(updateViews);
        setPostForRedis(key, post);
        return post;
    }

    @Override
    public PostForRedis incrementLikes(PostForRedis post) {
        String key = getKey(post.getId());
        String likesKey = getLikesKey(key);
        Long updateLikes = redisTemplate.opsForValue().increment(likesKey);
        redisTemplate.expire(likesKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        post.setUpdateLikes(updateLikes);
        setPostForRedis(key, post);
        return post;
    }

    @Override
    public PostForRedis decrementLikes(PostForRedis post) {
        String key = getKey(post.getId());
        String likesKey = getLikesKey(key);
        Long updateLikes = redisTemplate.opsForValue().decrement(likesKey);
        redisTemplate.expire(likesKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        post.setUpdateLikes(updateLikes);
        setPostForRedis(key, post);
        return post;
    }

    @Override
    public PostForRedis save(Post post) {
        String key = getKey(post.getId());
        PostForRedis postForRedis = PostForRedis.builder().post(post).build();
        setPostForRedis(key, postForRedis);
        return postForRedis;
    }

    @Override
    public void update(PostForRedis post) {
        String key = getKey(post.getId());
        setPostForRedis(key, post);
    }

    @Override
    public void delete(PostForRedis post) {
        String key = getKey(post.getId());
        String viewsKey = getViewsKey(key);
        String likesKey = getLikesKey(key);
        redisTemplate.delete(key);
        redisTemplate.delete(viewsKey);
        redisTemplate.delete(likesKey);
    }

    private PostForRedis getPostForRedis(String key) {
        return (PostForRedis) redisTemplate.opsForValue().get(key);
    }

    private void setPostForRedis(String key, PostForRedis postForRedis) {
        redisTemplate.opsForValue().set(key, postForRedis, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private String getKey(Long postId) {
        return POST_KEY + postId;
    }

    private String getViewsKey(String key) {
        return key + POST_VIEWS_KEY;
    }

    private String getLikesKey(String key) {
        return key + POST_LIKES_KEY;
    }
}
