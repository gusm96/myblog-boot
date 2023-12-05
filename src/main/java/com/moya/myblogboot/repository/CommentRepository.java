package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c from Comment c " +
            "where c.board.id =: boardId " +
            "order by c.write_date desc ")
    List<Comment> findAllByBoardId(Long boardId);
}
