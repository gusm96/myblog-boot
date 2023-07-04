package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.BoardReqDto;
import com.moya.myblogboot.domain.BoardResDto;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.service.BoardService;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<List> getAllBoards (@RequestParam(name = "page", defaultValue = "1") int page){
        List<BoardResDto> list = boardService.getBoardList(page);
        return ResponseEntity.ok().body(list);
    }
    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/{category}")
    public ResponseEntity<List> thisTypeOfPosts (@PathVariable("category") String category, @RequestParam(name = "page", defaultValue = "1") int page) {
        List<BoardResDto> list = boardService.getAllBoardsInThatCategory(category, page);
        return ResponseEntity.ok().body(list);
    }
    // 선택한 게시글
    @GetMapping("/api/v1/board/{boardId}")
    public ResponseEntity<BoardResDto> getPost(@PathVariable Long boardId) {
        BoardResDto board = boardService.getBoard(boardId);
        return ResponseEntity.ok().body(board);
    }
    // 게시글 작성 Post
    @PostMapping("/api/v1/management/board")
    public ResponseEntity<Long> newPost (@RequestBody @Valid BoardReqDto boardReqDto, HttpServletRequest request)  {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorization.split(" ")[1];
        try {
            Long boardId = boardService.uploadBoard(boardReqDto, token);
            return ResponseEntity.ok().body(boardId);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 관리자는 존재하지 않습니다.");
        } catch (ExpiredTokenException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다.");
        }
    }
    // 게시글 수정
    @PutMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Long> editPost (@PathVariable("boardId") Long boardId, @RequestBody @Valid BoardReqDto boardReqDto){
        return ResponseEntity.ok().body(boardService.editBoard(boardId, boardReqDto));
    }
    // 게시글 삭제
    @DeleteMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Boolean> deletePost(@PathVariable("boardId") Long boardId){
        // boardId 로 삭제 Service Logic 처리 후 결과 return
        return ResponseEntity.ok().body(boardService.deleteBoard(boardId));
    }
}
