package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardLikeService boardLikeService;

    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardList(getPage(page)));
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/{categoryName}")
    public ResponseEntity<BoardListResDto>   requestCategoryOfBoards(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListByCategory(categoryName, getPage(page)));
    }

    // 검색 결과 게시글 리스트
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> searchBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListBySearch(searchType, searchContents, getPage(page)));
    }

    // 게시글 상세v2
    @GetMapping("/api/v2/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.boardToResponseDto(boardId));
    }

    // 게시글 상세 V3
    @GetMapping("/api/v3/boards/{boardId}")
    public ResponseEntity<BoardResDtoV2> getBoardDetailV3(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveBoardDetail(boardId));
    }
    // 게시글 작성 Post
    @PostMapping("/api/v1/boards")
    public ResponseEntity<Long> postBoard(@RequestBody @Valid BoardReqDto boardReqDto, Principal principal) {

        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.uploadBoard(boardReqDto, memberId));
    }

    // 게시글 수정
    @PutMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Long> editBoard(@PathVariable("boardId") Long boardId,
                                          @RequestBody @Valid BoardReqDto boardReqDto,
                                          Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.editBoard(memberId, boardId, boardReqDto));
    }

    // 게시글 삭제
    @DeleteMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Boolean> deleteBoard( @PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.deleteBoard(boardId, memberId));
    }

    // 게시글 좋아요 여부 확인
    @GetMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToCheckBoardLike( @PathVariable("boardId") Long boardId, Principal principal){
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.isBoardLiked(memberId, boardId));
    }

    // 게시글 좋아요
    @PostMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToAddBoardLike(@PathVariable("boardId") Long boardId, Principal principal){
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLikeToBoard(memberId, boardId));
    }

    // 게시글 좋아요 V2
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> requestToAddBoardLikeV2(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLikeV2(boardId, memberId));
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/api/v1/likes/{boardId}")
    public ResponseEntity<?> requestToCancelBoardLike ( @PathVariable("boardId") Long boardId, Principal principal){
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.deleteBoardLike(memberId, boardId));
    }
    // 게시글 좋아요 취소 V2
    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> requestToCancelBoardLikeV2 ( @PathVariable("boardId") Long boardId, Principal principal){
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.cancelLikes(boardId, memberId));
    }

    private Long getMemberId(Principal principal) {
        Long memberId = -1L;
        if(principal instanceof UsernamePasswordAuthenticationToken){
            memberId = (Long) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return memberId;
    }

    private int getPage(int page) {
        return page - 1;
    }


}