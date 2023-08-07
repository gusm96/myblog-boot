package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.comment.CommentReqDTO;
import com.moya.myblogboot.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/api/v1/comment")
    public ResponseEntity<String> reqToWriteComment(@RequestBody @Valid CommentReqDTO commentReqDTO) {
        String result = commentService.writeComment(commentReqDTO);
        return ResponseEntity.ok().body(result);
    }
    // 댓글 수정
    @PutMapping("/api/v1/comment/{commentId}")
    public ResponseEntity<Boolean> editComment(@RequestBody @Valid CommentReqDTO commentReqDTO) {
        return ResponseEntity.ok().body(true);
    }
    // 댓글 삭제
    @DeleteMapping("/api/v1/comment/{commentid}")
    public ResponseEntity<Boolean> reqToDeleteComment(@PathVariable Long replyId) {
        return ResponseEntity.ok().body(commentService.deleteComment(replyId));
    }
    // 댓글 리스트
    @GetMapping("/api/v1/comments/{boardId}")
    public ResponseEntity<List> reqToCommentList(@PathVariable Long boardId) {
        return ResponseEntity.ok().body(commentService.getCommentList(boardId));
    }
}
