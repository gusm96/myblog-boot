package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;

import java.util.List;

public interface CommentService {
    String write(CommentReqDto commentReqDto, Long memberId, Long boardId);

    String update(Long commentId, Long memberId, String modifiedComment);

    String delete(Long commentId, Long memberId);

    List<CommentResDto> retrieveAll(Long boardId);

    List<CommentResDto> retrieveAllChild(Long parentId);

    Comment retrieve(Long commentId);
}
