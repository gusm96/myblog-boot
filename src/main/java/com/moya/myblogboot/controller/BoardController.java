package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.*;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
@Slf4j
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final BoardLikeService boardLikeService;

    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAll(getPage(page)));
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/category")
    public ResponseEntity<BoardListResDto> getCategoryBoards(
            @RequestParam("c") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        return ResponseEntity.ok().body(boardService.retrieveAllByCategory(category, getPage(page)));
    }

    // 검색 결과 게시글 리스트
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> getSearchedBoards(
            @RequestParam("type") SearchType searchType,
            @RequestParam("contents") String searchContents,
            @RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAllBySearched(searchType, searchContents, getPage(page)));
    }

    // 게시글 상세 V4
    @GetMapping("/api/v4/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveAndIncrementViewsDto(boardId));
    }

    // 게시글 상세 관리자용
    @GetMapping("/api/v1/management/boards/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetailForAdmin(@PathVariable("boardId") Long boardId) {
        return ResponseEntity.ok().body(boardService.retrieveDto(boardId));
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/boards")
    public ResponseEntity<Long> writeBoard(@RequestBody @Valid BoardReqDto boardReqDto, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.write(boardReqDto, memberId));
    }

    // 게시글 수정 PUT
    @PutMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Long> editBoard(@PathVariable("boardId") Long boardId,
                                          @RequestBody @Valid BoardReqDto boardReqDto,
                                          Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardService.edit(memberId, boardId, boardReqDto));
    }

    // 게시글 삭제 DELETE
    @DeleteMapping("/api/v1/boards/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        boardService.delete(boardId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 삭제 예정 게시글 리스트 GET
    @GetMapping("/api/v1/deleted-boards")
    public ResponseEntity<?> getDeletedBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAllDeleted(getPage(page)));
    }

    // 게시글 삭제 취소 PUT
    @PutMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> cancelDeletedBoard(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        boardService.undelete(boardId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 게시글 영구 삭제 DELETE
    @DeleteMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> deleteBoardPermanently(@PathVariable("boardId") Long boardId) {
        boardService.deletePermanently(boardId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 게시글 좋아요 여부 확인 GET
    @GetMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> checkBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.isLiked(boardId, memberId));
    }

    // 게시글 좋아요 POST
    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> addBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.addLikes(boardId, memberId));
    }

    // 게시글 좋아요 취소 DELETE
    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<?> cancelBoardLike(@PathVariable("boardId") Long boardId, Principal principal) {
        Long memberId = getMemberId(principal);
        return ResponseEntity.ok().body(boardLikeService.cancelLikes(boardId, memberId));
    }

    private Long getMemberId(Principal principal) {
        Long memberId = -1L;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            memberId = (Long) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        }
        return memberId;
    }

    private int getPage(int page) {
        return page - 1;
    }


}