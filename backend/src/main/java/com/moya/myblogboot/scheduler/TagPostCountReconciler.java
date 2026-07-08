package com.moya.myblogboot.scheduler;

import com.moya.myblogboot.domain.tag.Tag;
import com.moya.myblogboot.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagPostCountReconciler {

    private final TagRepository tagRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void reconcile() {
        for (Long tagId : tagRepository.findTagIdsWithMismatchedPostCount()) {
            Tag tag = tagRepository.findById(tagId).orElse(null);
            if (tag == null) {
                continue;
            }
            int actual = tagRepository.countActivePostsByTagId(tagId);
            log.warn("[TagPostCountReconciler] correcting tagId={} postCount={} actual={}",
                    tagId, tag.getPostCount(), actual);
            tag.resetPostCount(actual);
        }
    }
}
