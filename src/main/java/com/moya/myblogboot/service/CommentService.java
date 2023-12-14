package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;

import java.util.List;

public interface CommentService {
    String addComment(CommentReqDto commentReqDto, Long memberId, Long boardId);

    String updateComment(Long commentId, Long memberId, String modifiedComment);

    String deleteComment(Long commentId, Long memberId);

    List<CommentResDto> getCommentList(Long boardId);

    Comment retrieveCommentById(Long commentId);
}
