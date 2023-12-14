package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;
    private final AuthService authService;

    // 댓글 리스트
    @GetMapping("/api/v1/comments/{boardId}")
    public ResponseEntity<List> requestCommentList(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(commentService.getCommentList(boardId));
    }
    
    // 댓글 작성
    @PostMapping("/api/v1/comments/{boardId}")
    public ResponseEntity<String> requestCreateComment (HttpServletRequest request,
                                            @PathVariable("boardId") Long boardId,
                                            @RequestBody CommentReqDto commentReqDto ){
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(commentService.addComment( commentReqDto, memberId, boardId));
    }

    // 댓글 수정
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<String> requestEditComment(HttpServletRequest request,
                                             @PathVariable("commentId") Long commentId,
                                             @RequestBody CommentReqDto commentReqDto) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(commentService.updateComment(commentId, memberId, commentReqDto.getComment()));
    }

    // 댓글 삭제
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<String> requestToDeleteComment(
            HttpServletRequest request,
            @PathVariable("commentId") Long commentId) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(commentService.deleteComment(commentId, memberId));
    }

    private TokenInfo getTokenInfo(HttpServletRequest req){
        return authService.getTokenInfo(req.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1]);
    }

}
