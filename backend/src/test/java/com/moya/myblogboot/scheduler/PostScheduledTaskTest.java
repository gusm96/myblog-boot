package com.moya.myblogboot.scheduler;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostForRedis;
import com.moya.myblogboot.repository.PostRedisRepository;
import com.moya.myblogboot.service.PostCacheService;
import com.moya.myblogboot.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostScheduledTaskTest {

    @InjectMocks
    private PostScheduledTask postScheduledTask;

    @Mock
    private PostRedisRepository postRedisRepository;

    @Mock
    private PostCacheService postCacheService;

    @Mock
    private PostService postService;

    @Test
    @DisplayName("캐시에 데이터가 있으면 DB에 동기화하고 캐시를 삭제한다")
    void updateFromRedisStoreToDB_success() {
        // given
        Set<Long> keys = Set.of(1L, 2L);
        given(postRedisRepository.getKeys(anyString())).willReturn(keys);

        Post post1 = Post.builder().title("title1").content("content1").build();
        Post post2 = Post.builder().title("title2").content("content2").build();

        PostForRedis cache1 = new PostForRedis(post1);
        PostForRedis cache2 = new PostForRedis(post2);

        given(postCacheService.getPostFromCache(1L)).willReturn(cache1);
        given(postCacheService.getPostFromCache(2L)).willReturn(cache2);
        given(postService.findById(any())).willReturn(post1, post2);

        // when
        postScheduledTask.updateFromRedisStoreToDB();

        // then
        verify(postRedisRepository, times(2)).delete(any(PostForRedis.class));
        verify(postService, times(2)).findById(any());
    }

    @Test
    @DisplayName("캐시에 데이터가 없으면 아무 작업도 하지 않는다")
    void updateFromRedisStoreToDB_emptyKeys() {
        // given
        given(postRedisRepository.getKeys(anyString())).willReturn(Collections.emptySet());

        // when
        postScheduledTask.updateFromRedisStoreToDB();

        // then
        verify(postCacheService, never()).getPostFromCache(any());
        verify(postService, never()).findById(any());
    }

    @Test
    @DisplayName("개별 게시글 동기화 실패 시 나머지 게시글은 계속 처리한다")
    void updateFromRedisStoreToDB_partialFailure() {
        // given
        Set<Long> keys = Set.of(1L, 2L, 3L);
        given(postRedisRepository.getKeys(anyString())).willReturn(keys);

        Post post = Post.builder().title("title").content("content").build();
        PostForRedis validCache = new PostForRedis(post);

        // 1L: 성공, 2L: 예외 발생, 3L: 성공
        given(postCacheService.getPostFromCache(1L)).willReturn(validCache);
        given(postCacheService.getPostFromCache(2L)).willThrow(new RuntimeException("Redis connection error"));
        given(postCacheService.getPostFromCache(3L)).willReturn(validCache);
        given(postService.findById(any())).willReturn(post);

        // when
        postScheduledTask.updateFromRedisStoreToDB();

        // then - 2L 실패해도 1L, 3L은 처리됨
        verify(postCacheService, times(3)).getPostFromCache(any());
        verify(postService, times(2)).findById(any());
    }

    @Test
    @DisplayName("getPostFromCache가 null을 반환하면 해당 게시글을 건너뛴다")
    void updateFromRedisStoreToDB_nullCache() {
        // given
        Set<Long> keys = Set.of(1L);
        given(postRedisRepository.getKeys(anyString())).willReturn(keys);
        given(postCacheService.getPostFromCache(1L)).willReturn(null);

        // when
        postScheduledTask.updateFromRedisStoreToDB();

        // then
        verify(postService, never()).findById(any());
        verify(postRedisRepository, never()).delete(any(PostForRedis.class));
    }
}
