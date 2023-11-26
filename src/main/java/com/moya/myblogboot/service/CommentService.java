package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;

import java.util.List;

public interface CommentService {
    String addComment(Member member, Board board, CommentReqDto commentReqDto);

    String updateComment(Long memberId, Long commentId, String modifiedComment);

    boolean deleteComment(Long memberId, Long commentId, Board board);

    List<CommentResDto> getCommentList(Long boardId);

    Comment retrieveCommentById(Long commentId);
}
