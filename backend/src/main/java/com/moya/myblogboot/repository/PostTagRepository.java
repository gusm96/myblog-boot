package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.tag.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    long countByTagId(Long tagId);

    @Query("select count(pt) from PostTag pt where pt.tag.id = :tagId and pt.post.deleteDate is null")
    long countByTagIdAndPostNotDeleted(@Param("tagId") Long tagId);

    List<PostTag> findAllByTagId(Long tagId);

    boolean existsByPostIdAndTagId(Long postId, Long tagId);
}
