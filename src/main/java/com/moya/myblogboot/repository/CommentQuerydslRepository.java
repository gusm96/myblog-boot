package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.comment.CommentResDto;

import java.util.List;

public interface CommentQuerydslRepository {

    List<CommentResDto> findAllByBoardId(Long boardId);

    List<CommentResDto> findChildByParentId(Long parentId);
}
