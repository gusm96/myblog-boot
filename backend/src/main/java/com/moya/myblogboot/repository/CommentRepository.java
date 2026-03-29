package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentQuerydslRepository{

    @Query("select c from Comment c join fetch c.member where c.id = :id")
    Optional<Comment> findByIdWithMember(@Param("id") Long id);
}
