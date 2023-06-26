package com.moya.myblogboot.repository;

import com.moya.myblogboot.domain.Reply;

import java.util.List;
import java.util.Optional;

public interface ReplyRepositoryInf {
    // 댓글 작성
    Long write(Reply reply);
    // 댓글 찾기
    Optional<Reply> findOne(Long replyId);
    // 댓글 리스트
    List<Reply> replyList(Long boardId);
    // 댓글 삭제
    void removeReply(Long replyId);
}
