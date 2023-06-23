package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Reply;

import java.util.List;

public interface ReplyRepositoryInf {
    // 댓글 작성
    Long write(Reply reply);
    // 댓글 찾기
    Reply findOne(Long replyId);
    // 댓글 리스트
    List<Reply> replyList(Long boardId);

}
