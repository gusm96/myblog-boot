package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.tag.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long>, TagQuerydslRepository {
    Optional<Tag> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Tag> findAllBySlugIn(Collection<String> slugs);
    List<Tag> findByNameContainingIgnoreCase(String name);
    List<Tag> findAllByPostCountGreaterThanOrderByPostCountDesc(int postCount);
    List<Tag> findByPostCountGreaterThanOrderByPostCountDesc(int postCount, Pageable pageable);
}
