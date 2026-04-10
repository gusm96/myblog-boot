package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.comment.*;
import com.moya.myblogboot.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/v1/comments/{postId}")
    public ResponseEntity<List<CommentResDto>> getComments(@PathVariable("postId") Long postId) {
        return ResponseEntity.ok(commentService.retrieveAll(postId));
    }

    @GetMapping("/api/v1/comments/child/{parentId}")
    public ResponseEntity<List<CommentResDto>> getChildComments(@PathVariable("parentId") Long parentId) {
        return ResponseEntity.ok(commentService.retrieveAllChild(parentId));
    }

    // JWT가 없으면 비회원, JWT가 있으면 어드민으로 처리
    @PostMapping("/api/v1/comments/{postId}")
    public ResponseEntity<CommentWriteResDto> writeComment(
            @PathVariable("postId") Long postId,
            @RequestBody @Valid CommentReqDto commentReqDto,
            Principal principal) {
        boolean isAdmin = principal != null;
        return ResponseEntity.ok(commentService.write(commentReqDto, postId, isAdmin));
    }

    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> editComment(
            @PathVariable("commentId") Long commentId,
            @RequestBody @Valid CommentUpdateReqDto reqDto,
            Principal principal) {
        boolean isAdmin = principal != null;
        commentService.update(commentId, reqDto, isAdmin);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") Long commentId,
            @RequestBody CommentDeleteReqDto reqDto,
            Principal principal) {
        boolean isAdmin = principal != null;
        commentService.delete(commentId, reqDto, isAdmin);
        return ResponseEntity.ok().build();
    }
}
