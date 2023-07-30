package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.comment.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepositoryInf {
    // 댓글 작성
    Long write(Comment comment);
    // 댓글 찾기
    Optional<Comment> findOne(Long replyId);
    // 댓글 리스트
    List<Comment> commentList(Long boardId);
    // 댓글 삭제
    void removeComment(Long replyId);
}
