package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.post.PostSlugHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostSlugHistoryRepository extends JpaRepository<PostSlugHistory, Long> {

    Optional<PostSlugHistory> findByOldSlug(String oldSlug);

    boolean existsByOldSlug(String oldSlug);
}
