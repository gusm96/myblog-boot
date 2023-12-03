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
    private final BoardService boardService;
    private final AuthService authService;

    // 댓글 작성
    @PostMapping("/api/v1/comments/{boardId}")
    public ResponseEntity<String> createComment (HttpServletRequest request,
                                            @PathVariable("boardId") Long boardId,
                                            @RequestBody CommentReqDto commentReqDto ){
        Board board = getBoard(boardId);
        Member member = getMember(getTokenInfo(request).getMemberPrimaryKey());
        return ResponseEntity.ok().body(commentService.addComment(member, board, commentReqDto));
    }

    // update
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<String> modifiedComment(HttpServletRequest request,
                                             @PathVariable("commentId") Long commentId,
                                             @RequestBody CommentReqDto commentReqDto) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(commentService.updateComment(memberId, commentId, commentReqDto.getComment()));
    }

    // delete
    @DeleteMapping("/api/v1/boards/{boardId}/comments/{commentId}")
    public ResponseEntity<String> requestToDeleteComment(
            HttpServletRequest request,
            @PathVariable("boardId") Long boardId,
            @PathVariable("commentId") Long commentId) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        Board board = getBoard(boardId);
        return ResponseEntity.ok().body(commentService.deleteComment(memberId, commentId, board));
    }

    private TokenInfo getTokenInfo(HttpServletRequest req){
        return authService.getTokenInfo(req.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1]);
    }

    private Member getMember(Long memberId) {
        return authService.retrieveMemberById(memberId);
    }

    private Board getBoard (Long boardId){
        return boardService.retrieveBoardById(boardId);
    }
}
