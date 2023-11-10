package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final AuthService authService;
    private final CommentService commentService;
    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardList(page));
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/{category}")
    public ResponseEntity<BoardListResDto> thisTypeOfBoards(
            @PathVariable("category") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListByCategory(category, page));
    }

    // 선택한 게시글
    @GetMapping("/api/v1/board/{boardId}")
    public ResponseEntity<?> getBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveBoardResponseById(boardId));
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/management/board")
    public ResponseEntity<Long> postBoard(HttpServletRequest request, @RequestBody @Valid BoardReqDto boardReqDto) {
        return ResponseEntity.ok().body(boardService.uploadBoard(boardReqDto, getTokenInfo(request).getMemberPrimaryKey()));
    }

    // 게시글 수정
    @PutMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Long> editBoard(@PathVariable("boardId") Long boardId, @RequestBody @Valid BoardReqDto boardReqDto) {
        return ResponseEntity.ok().body(boardService.editBoard(boardId, boardReqDto));
    }

    // 게시글 삭제
    @DeleteMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.deleteBoard(boardId));
    }

    // 게시글 검색 기능 추가
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> searchBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListBySearch(searchType, searchContents, page));
    }

    // 게시글 좋아요 여부
    @GetMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<?> requestToCheckBoardLike(HttpServletRequest request, @PathVariable("boardId") Long boardId){
        return ResponseEntity.ok().body(boardService.checkBoardLikedStatus(getTokenInfo(request).getMemberPrimaryKey(), boardId));
    }
    // 게시글 좋아요 추가 기능
    @PostMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<?> requestToAddBoardLike(HttpServletRequest request, @PathVariable("boardId") Long boardId){
        return ResponseEntity.ok().body(boardService.addLikeToBoard(getTokenInfo(request).getMemberPrimaryKey(), boardId));
    }

    @DeleteMapping("/api/v1/boards/{boardId}/likes")
    public ResponseEntity<?> requestToDeleteBoardLike (HttpServletRequest request, @PathVariable("boardId") Long boardId){
        return ResponseEntity.ok().body(boardService.deleteBoardLike(getTokenInfo(request).getMemberPrimaryKey(), boardId));
    }

    // 댓글 리스트
    @GetMapping("/api/v1/boards/{boardId}/comments")
    public ResponseEntity<List> reqToCommentList(@PathVariable Long boardId) {
        return ResponseEntity.ok().body(commentService.getCommentList(boardId));
    }
    // 댓글 작성
    @PostMapping("/api/v1/boards/{boardId}/comments")
    public ResponseEntity<?> requestToWriteComment(
            HttpServletRequest request,
            @PathVariable("boardId") Long boardId,
            @RequestBody @Valid CommentReqDto commentReqDto) {

        Member member = authService.retrieveMemberById(getTokenInfo(request).getMemberPrimaryKey());
        Board board = boardService.retrieveBoardById(boardId);

        return ResponseEntity.ok().body(commentService.writeComment(member, board, commentReqDto));
    }
    // 댓글 수정
    @PutMapping("/api/v1/boards/{boardId}/comments/{commentId}")
    public ResponseEntity<?> editComment(@RequestBody @Valid CommentReqDto commentReqDTO) {
        return ResponseEntity.ok().body(true);
    }
    // 댓글 삭제
    @DeleteMapping("/api/v1/boards/{boardId}/comments/{commentId}")
    public ResponseEntity<?> requestToDeleteComment(
            HttpServletRequest request,
            @PathVariable("boardId") Long boardId,
            @PathVariable("commentId") Long commentId) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        Board board = boardService.retrieveBoardById(boardId);
        return ResponseEntity.ok().body(commentService.deleteComment(memberId, commentId, board));
    }

    private TokenInfo getTokenInfo(HttpServletRequest req){
        return authService.getTokenInfo(req.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1]);
    }

}