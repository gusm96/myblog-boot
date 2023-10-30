package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.board.BoardListResDto;
import com.moya.myblogboot.domain.board.BoardReqDto;
import com.moya.myblogboot.domain.board.BoardResDto;
import com.moya.myblogboot.domain.board.SearchType;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.service.BoardService;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<BoardListResDto> getAllBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        BoardListResDto result = boardService.getBoardList(page);
        return ResponseEntity.ok().body(result);
    }

    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/{category}")
    public ResponseEntity<BoardListResDto> thisTypeOfBoards(
            @PathVariable("category") String category,
            @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        BoardListResDto result = boardService.getBoardsByCategory(category, page);
        return ResponseEntity.ok().body(result);
    }

    // 선택한 게시글
    @GetMapping("/api/v1/board/{boardId}")
    public ResponseEntity<BoardResDto> getBoard(@PathVariable Long boardId) {
        BoardResDto board = boardService.getBoard(boardId);
        return ResponseEntity.ok().body(board);
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/management/board")
    public ResponseEntity<Long> postBoard(@RequestBody @Valid BoardReqDto boardReqDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getPrincipal().toString();
        Long boardId = boardService.uploadBoard(boardReqDto, username);
        return ResponseEntity.ok().body(boardId);
    }

    // 게시글 수정
    @PutMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Long> editBoard(@PathVariable("boardId") Long boardId, @RequestBody @Valid BoardReqDto boardReqDto) {
        Long result = boardService.editBoard(boardId, boardReqDto);
        return ResponseEntity.ok().body(result);
    }

    // 게시글 삭제
    @DeleteMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Boolean> deleteBoard(@PathVariable("boardId") Long boardId) {
        // boardId 로 삭제 Service Logic 처리 후 결과 return
        boolean result = boardService.deleteBoard(boardId);
        return ResponseEntity.ok().body(result);
    }

    // 게시글 검색 기능 추가
    @GetMapping("/api/v1/boards/search")
    public ResponseEntity<BoardListResDto> searchBoards(@RequestParam("type") SearchType searchType,
                                                        @RequestParam("contents") String searchContents,
                                                        @RequestParam(name = "p", defaultValue = "1") int page
    ) {
        BoardListResDto result = boardService.getBoardsBySearch(searchType, searchContents, page);
        return ResponseEntity.ok().body(result);
    }

    // 게시글 좋아요 기능
}