package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardDto;
import com.moya.myblogboot.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BoardController {

    private BoardService service;
    @Autowired
    public BoardController(BoardService service) {
        this.service = service;
    }
    // 모든 게시글
    @GetMapping("/api/v1/boards")
    public ResponseEntity<List> getAllBoards (@RequestParam(name = "page", defaultValue = "1") int page){
        List<Board> list = service.getBoardList(page);
        return ResponseEntity.ok().body(list);
    }
    @GetMapping("/api/v1/boards/{category}")
    public ResponseEntity<List> thisTypeOfPosts (@PathVariable("category") String category, @RequestParam(name = "page", defaultValue = "1") int page) {
        List<Board> list = service.getAllBoardsInThatCategory(category, page);
        return ResponseEntity.ok().body(list);
    }


    @GetMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Board> getPost(@PathVariable("boardId") long boardId) {
        // boardId 값으로 해당 게시글 찾아서 return
        Board board = service.getBoard(boardId);
        return ResponseEntity.ok().body(board);
    }

    // 게시글 작성 Post
    @PostMapping("/api/v1/management/board")
    public ResponseEntity<Long> newPost (@RequestBody BoardDto boardDto) {
        // 받아온 Token으로 권한 검증
        // Token으로 Admin 정보 찾아온다.
        // Admin 정보와 함께 Board persist();
        Admin admin = null;
        // 게시글 업로드를 성공하면 해당 식별값 response
        Long boardId = service.newPost(boardDto, admin);
        return ResponseEntity.ok().body(boardId);
    }
    @PostMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<Long> editPost (@PathVariable("boardId") Long boardId, @RequestBody BoardDto boardDto){
        return ResponseEntity.ok().body(service.editBoard(boardId, boardDto));
    }
    @DeleteMapping("/api/v1/management/board/{boardId}")
    public ResponseEntity<String> deletePost(@PathVariable("boardId") Long boardId){
        // boardId 로 삭제 Service Logic 처리 후 결과 return
        String result = "";
        return ResponseEntity.ok().body(result);
    }
}
