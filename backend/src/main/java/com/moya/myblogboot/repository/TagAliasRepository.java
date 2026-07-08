package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.tag.TagAlias;
import com.moya.myblogboot.domain.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TagAliasRepository extends JpaRepository<TagAlias, String> {
    Optional<TagAlias> findByFromSlug(String fromSlug);

    @Query("select count(a) from TagAlias a where a.toTag.id = :tagId")
    long countByToTagId(@Param("tagId") Long tagId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TagAlias a set a.toTag = :dstTag where a.toTag = :srcTag")
    int repointTo(@Param("srcTag") Tag srcTag, @Param("dstTag") Tag dstTag);
}
