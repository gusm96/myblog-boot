package com.moya.myblogboot.repository;

import com.moya.myblogboot.dto.comment.CommentResDto;

import java.util.List;

public interface CommentQuerydslRepository {

    List<CommentResDto> findAllByPostId(Long postId);

    List<CommentResDto> findChildByParentId(Long parentId);
}
