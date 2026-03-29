package com.moya.myblogboot.controller;

import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.utils.PrincipalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeService boardLikeService;
    private final BoardService boardService;

    // 게시글 좋아요 여부 확인
    @GetMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> checkBoardLike(@PathVariable("boardId") Long boardId,
                                            Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.isLiked(boardId, memberId));
    }

    // 게시글 좋아요
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> addBoardLike(@PathVariable("boardId") Long boardId,
                                             Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLikes(boardId, memberId));
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> cancelBoardLike(@PathVariable("boardId") Long boardId,
                                             Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.cancelLikes(boardId, memberId));
    }

    // 조회수 조회
    @GetMapping("/api/v1/boards/{boardId}/views")
    public ResponseEntity<Long> getViews(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId).getViews());
    }

    // 좋아요수 조회
    @GetMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<Long> getLikes(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.getBoardDetail(boardId).getLikes());
    }
}
