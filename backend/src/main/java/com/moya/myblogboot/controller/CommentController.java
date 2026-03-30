package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.service.CommentService;
import com.moya.myblogboot.utils.PrincipalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;

    // 댓글 리스트
    @GetMapping("/api/v1/comments/{boardId}")
    public ResponseEntity<List<CommentResDto>> getComments(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(commentService.retrieveAll(boardId));
    }

    // 자식 댓글 리스트
    @GetMapping("/api/v1/comments/child/{parentId}")
    public ResponseEntity<List<CommentResDto>> getChildComments(@PathVariable("parentId") Long parentId) {
        return ResponseEntity.ok().body(commentService.retrieveAllChild(parentId));
    }

    // 댓글 작성
    @PostMapping("/api/v1/comments/{boardId}")
    public ResponseEntity<Void> writeComment(Principal principal,
                                             @PathVariable("boardId") Long boardId,
                                             @RequestBody CommentReqDto commentReqDto) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        commentService.write(commentReqDto, memberId, boardId);
        return ResponseEntity.ok().build();
    }

    // 댓글 수정
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> editComment(Principal principal,
                                            @PathVariable("commentId") Long commentId,
                                            @RequestBody CommentReqDto commentReqDto) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        commentService.update(commentId, memberId, commentReqDto.getComment());
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            Principal principal,
            @PathVariable("commentId") Long commentId) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        commentService.delete(commentId, memberId);
        return ResponseEntity.ok().build();
    }
}
