package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.comment.*;

import java.util.List;

public interface CommentService {

    CommentWriteResDto write(CommentReqDto reqDto, Long boardId, boolean isAdmin);

    void update(Long commentId, CommentUpdateReqDto reqDto, boolean isAdmin);

    void delete(Long commentId, CommentDeleteReqDto reqDto, boolean isAdmin);

    List<CommentResDto> retrieveAll(Long boardId);

    List<CommentResDto> retrieveAllChild(Long parentId);

    Comment retrieve(Long commentId);
}
