package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.dto.comment.CommentDeleteReqDto;
import com.moya.myblogboot.dto.comment.CommentReqDto;
import com.moya.myblogboot.dto.comment.CommentResDto;
import com.moya.myblogboot.dto.comment.CommentUpdateReqDto;
import com.moya.myblogboot.dto.comment.CommentWriteResDto;

import java.util.List;

public interface CommentService {

    CommentWriteResDto write(CommentReqDto reqDto, Long postId, boolean isAdmin);

    void update(Long commentId, CommentUpdateReqDto reqDto, boolean isAdmin);

    void delete(Long commentId, CommentDeleteReqDto reqDto, boolean isAdmin);

    List<CommentResDto> retrieveAll(Long postId);

    List<CommentResDto> retrieveAllChild(Long parentId);

    Comment retrieve(Long commentId);
}
