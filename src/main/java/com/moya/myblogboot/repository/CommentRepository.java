package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.parent " +
            "LEFT JOIN FETCH c.child " +
            "WHERE c.board.id = :boardId " +
            "AND (c.parent IS NULL OR c.parent.id IS NULL) " +
            "ORDER BY c.write_date DESC")
    List<Comment> findAllByBoardId(Long boardId);

}
