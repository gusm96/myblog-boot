package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.event.CategoryChangeEvent;
import com.moya.myblogboot.domain.event.PostChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "revalidate.webhook", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RevalidateWebhookListener {

    private final RevalidateWebhookClient client;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostChange(PostChangeEvent event) {
        List<String> tags = new ArrayList<>();
        List<String> paths = new ArrayList<>();

        tags.add("posts");

        String slug = event.getSlug();
        String changeType = event.getChangeType();

        if (slug != null && !slug.isBlank()) {
            if ("UPDATED".equals(changeType) || "DELETED".equals(changeType)) {
                tags.add("post:" + slug);
            }
        }
        if ("CREATED".equals(changeType) || "DELETED".equals(changeType)) {
            tags.add("slugs");
            paths.add("/sitemap.xml");
        }

        client.send(tags, paths);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryChange(CategoryChangeEvent event) {
        client.send(List.of("categories"), List.of());
    }
}
