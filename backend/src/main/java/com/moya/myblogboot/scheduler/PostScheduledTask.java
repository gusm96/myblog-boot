package com.moya.myblogboot.scheduler;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.dto.post.PostForRedis;
import com.moya.myblogboot.repository.PostRedisRepository;
import com.moya.myblogboot.service.PostCacheService;
import com.moya.myblogboot.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.moya.myblogboot.domain.keys.RedisKey.*;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class PostScheduledTask {
    private final PostRedisRepository postRedisRepository;
    private final PostCacheService postCacheService;
    private final PostService postService;
    private final Lock lock = new ReentrantLock();
    private static final Long SECONDS_IN_15DAYS = 15L * 24L * 60L * 60L; // 15일

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행되도록 스케줄링
    @Transactional
    public void deleteExpiredPosts() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_IN_15DAYS);
        postService.deletePermanently(thresholdDate);
        log.info("삭제 후 15일이 지난 게시글 영구삭제");
    }

    /**
     * 10분마다 Redis 캐시에 저장된 게시글 조회수/좋아요수를 DB에 동기화하고 캐시를 정리한다.
     * - ReentrantLock으로 자정 작업과의 동시 실행을 방지한다.
     * - 개별 게시글 단위로 에러를 처리해 하나의 실패가 전체 동기화를 중단하지 않는다.
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateFromRedisStoreToDB() {
        if (!lock.tryLock()) {
            log.debug("게시글 캐시 동기화 스킵: 이전 작업 진행 중");
            return;
        }
        try {
            String keyPattern = POST_KEY + "*";
            Set<Long> keys = postRedisRepository.getKeys(keyPattern);
            if (keys.isEmpty()) {
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (Long postId : keys) {
                try {
                    PostForRedis postForRedis = postCacheService.getPostFromCache(postId);
                    if (postForRedis != null) {
                        updatePost(postForRedis);
                        deleteFromCache(postId, postForRedis);
                        successCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("게시글 캐시 동기화 실패 [postId={}]: {}", postId, e.getMessage());
                }
            }

            if (failCount == 0) {
                log.info("게시글 캐시 → DB 동기화 완료: {}건 처리", successCount);
            } else {
                log.warn("게시글 캐시 → DB 동기화 완료: 성공 {}건, 실패 {}건", successCount, failCount);
            }
        } finally {
            lock.unlock();
        }
    }

    private void updatePost(PostForRedis postForRedis) {
        Post post = postService.findById(postForRedis.getId());
        post.updateViews(postForRedis.totalViews());
        post.updateLikes(postForRedis.totalLikes());
    }

    private void deleteFromCache(Long postId, PostForRedis postForRedis) {
        try {
            postRedisRepository.delete(postForRedis);
        } catch (Exception e) {
            log.error("캐시 삭제 실패 [postId={}]: {}", postId, e.getMessage());
        }
    }
}
