package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.post.Post;
import com.moya.myblogboot.domain.post.PostStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostQuerydslRepository {

    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query("select p.id from Post p where p.slug = :slug")
    Optional<Long> findIdBySlug(@Param("slug") String slug);

    @Query("select p from Post p " +
            "join fetch p.admin " +
            "join fetch p.category where p.id = :postId")
    Optional<Post> findById(@Param("postId") Long postId);

    @Query(value = "select p from Post p join fetch p.admin join fetch p.category where p.postStatus = :postStatus",
            countQuery = "select count(p) from Post p where p.postStatus = :postStatus")
    Page<Post> findAll(@Param("postStatus") PostStatus postStatus, Pageable pageable);

    @Query(value = "select p from Post p join fetch p.admin join fetch p.category " +
            "where p.category.name = :categoryName and p.postStatus = 'VIEW'",
            countQuery = "select count(p) from Post p where p.category.name = :categoryName and p.postStatus = 'VIEW'")
    Page<Post> findAllByCategoryName(@Param("categoryName") String categoryName, PageRequest pageRequest);

    @Query(value = "select p from Post p join fetch p.admin join fetch p.category " +
            "where p.deleteDate is not null",
            countQuery = "select count(p) from Post p where p.deleteDate is not null")
    Page<Post> findByDeletionStatus(PageRequest pageRequest);
}
