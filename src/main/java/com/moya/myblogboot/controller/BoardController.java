package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardReqDto;
import com.moya.myblogboot.domain.BoardResDto;
import com.moya.myblogboot.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService service;
    // 모든 게시글 리스트
    @GetMapping("/api/v1/boards")
    public ResponseEntity<List> getAllBoards (@RequestParam(name = "page", defaultValue = "1") int page){
        List<BoardResDto> list = service.getBoardList(page);
        return ResponseEntity.ok().body(list);
    }
    // 카테고리별 게시글 리스트
    @GetMapping("/api/v1/boards/{category}")
    public ResponseEntity<List> thisTypeOfPosts (@PathVariable("category") String category, @RequestParam(name = "page", defaultValue = "1") int page) {
        List<BoardResDto> list = service.getAllBoardsInThatCategory(category, page);
        return ResponseEntity.ok().body(list);
    }
    // 선택한 게시글
    @GetMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<BoardResDto> getPost(@PathVariable Long boardId) {
        // boardId 값으로 해당 게시글 찾아서 return
        BoardResDto board = service.getBoard(boardId);
        return ResponseEntity.ok().body(board);
    }
    // 게시글 작성 Post
    @PostMapping("/api/v1/management/board")
    public ResponseEntity<Long> newPost (@RequestBody BoardReqDto boardReqDto) {
        Long boardId = service.uploadBoard(boardReqDto);
        return ResponseEntity.ok().body(boardId);
    }
    // 게시글 수정
    @PostMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Long> editPost (@PathVariable("boardId") Long boardId, @RequestBody BoardReqDto boardReqDto){
        return ResponseEntity.ok().body(service.editBoard(boardId, boardReqDto));
    }
    // 게시글 삭제
    @DeleteMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<String> deletePost(@PathVariable("boardId") Long boardId){
        // boardId 로 삭제 Service Logic 처리 후 결과 return
        String result = "";
        return ResponseEntity.ok().body(result);
    }
}
