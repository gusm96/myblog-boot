package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentQuerydslRepository {

    boolean existsByPost_IdAndNicknameAndDiscriminator(Long postId, String nickname, String discriminator);
}
