package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final AuthService authService;

    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardList(getPage(page)));
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/categories/{categoryName}")
    public ResponseEntity<BoardListResDto>   requestCategoryOfBoards(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListByCategory(categoryName, getPage(page)));
    }

  /*  // 검색 결과 게시글 리스트
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> searchBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListBySearch(searchType, searchContents, getPage(page)));
    }*/

    // 게시글 상세v2
    @GetMapping("/api/v2/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.boardToResponseDto(boardId));
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/boards")
    public ResponseEntity<Long> postBoard(HttpServletRequest request, @RequestBody @Valid BoardReqDto boardReqDto) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.uploadBoard(boardReqDto, memberId));
    }

    // 게시글 수정
    @PutMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Long> editBoard(HttpServletRequest request,
                                          @PathVariable("boardId") Long boardId,
                                          @RequestBody @Valid BoardReqDto boardReqDto) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.editBoard(memberId, boardId, boardReqDto));
    }

    // 게시글 삭제
    @DeleteMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(HttpServletRequest request, @PathVariable("boardId") Long boardId) {
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.deleteBoard(boardId, memberId));
    }

    // 게시글 좋아요 여부 확인
    @GetMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToCheckBoardLike(HttpServletRequest request, @PathVariable("boardId") Long boardId){
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.isBoardLiked(memberId, boardId));
    }
    // 게시글 좋아요 추가 기능
    @PostMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToAddBoardLike(HttpServletRequest request, @PathVariable("boardId") Long boardId){
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.addLikeToBoard(memberId, boardId));
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToCancelBoardLike (HttpServletRequest request, @PathVariable("boardId") Long boardId){
        Long memberId = getTokenInfo(request).getMemberPrimaryKey();
        return ResponseEntity.ok().body(boardService.deleteBoardLike(memberId, boardId));
    }

    // 토큰 정보 조회
    private TokenInfo getTokenInfo(HttpServletRequest req){
        return authService.getTokenInfo(req.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1]);
    }

    private int getPage(int page) {
        return page - 1;
    }
}