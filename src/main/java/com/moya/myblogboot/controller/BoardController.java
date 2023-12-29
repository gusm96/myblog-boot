package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @GetMapping("/api/v1/boards/{category}")
    public ResponseEntity<BoardListResDto> requestCategoryOfBoards(
            @PathVariable("category") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListByCategory(category, getPage(page)));
    }

    // 검색 결과 게시글 리스트
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> searchBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveBoardListBySearch(searchType, searchContents, getPage(page)));
    }

    // 게시글 상세 V4
    @GetMapping("/api/v4/boards/{boardId}")
    public ResponseEntity<BoardResDtoV2> getBoardDetailV4(@PathVariable("boardId") Long boardId) {
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
    @GetMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> requestToCheckBoardLike( @PathVariable("boardId") Long boardId, Principal principal){
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.isBoardLiked(boardId, memberId));
    }

    // 게시글 좋아요
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> requestToAddBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLike(boardId, memberId));
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> requestToCancelBoardLike ( @PathVariable("boardId") Long boardId, Principal principal){
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.cancelLikes(boardId, memberId));
    }

    // 삭제 예정 게시글 리스트
    @GetMapping("/api/v1/deleted-boards")
    public ResponseEntity<?> requestDeletedBoards(@RequestParam(name = "p", defaultValue = "1") int page){
        return ResponseEntity.ok().body(boardService.retrieveDeletedBoards(getPage(page)));
    }

    // 게시글 삭제 취소
    @PutMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> requestCancelDeletedBoard(@PathVariable("boardId") Long boardId) {
        boardService.undeleteBoard(boardId);
        return ResponseEntity.ok().body("게시글 삭제 취소 완료");
    }

    // 게시글 영구 삭제
    @DeleteMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> requestDeleteBoard(@PathVariable("boardId")Long boardId) {
        boardService.deletePermanently(boardId);
        return ResponseEntity.ok("게시글이 영구 삭제되었습니다.");
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