package com.moya.myblogboot.domain.tag;

import com.moya.myblogboot.repository.TagRepository;
import com.moya.myblogboot.utils.TagNormalizer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class TagBackfillRunner {

    private final EntityManager em;
    private final TagRepository tagRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void run() {
        if (tagRepository.count() > 0 || !categoryTableExists()) {
            log.info("[TagBackfill] skipped.");
            return;
        }

        List<Object[]> categories = em.createNativeQuery("SELECT category_id, name FROM category").getResultList();
        Map<Long, Tag> categoryToTag = new HashMap<>();
        for (Object[] row : categories) {
            Long categoryId = ((Number) row[0]).longValue();
            String name = (String) row[1];
            Tag tag = tagRepository.save(Tag.builder()
                    .name(TagNormalizer.normalizeName(name))
                    .slug(TagNormalizer.toSlug(name))
                    .build());
            categoryToTag.put(categoryId, tag);
        }

        List<Object[]> posts = em.createNativeQuery(
                "SELECT post_id, category_id FROM post WHERE delete_date IS NULL").getResultList();
        for (Object[] row : posts) {
            Long postId = ((Number) row[0]).longValue();
            Long categoryId = ((Number) row[1]).longValue();
            Tag tag = categoryToTag.get(categoryId);
            if (tag == null) {
                continue;
            }
            em.createNativeQuery(
                    "INSERT INTO post_tag (post_id, tag_id, sort_order, is_primary, create_date) " +
                            "VALUES (?, ?, 0, 0, NOW())")
                    .setParameter(1, postId)
                    .setParameter(2, tag.getId())
                    .executeUpdate();
            tag.incrementPostCount();
        }
        log.info("[TagBackfill] migrated {} categories, {} posts.", categoryToTag.size(), posts.size());
    }

    private boolean categoryTableExists() {
        try {
            em.createNativeQuery("SELECT 1 FROM category LIMIT 1").getResultList();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
