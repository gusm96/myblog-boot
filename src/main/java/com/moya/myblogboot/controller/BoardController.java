package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.BoardReq;
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
    @GetMapping("/api/v1/posts")
    public ResponseEntity<List> allPosts (@RequestParam(name = "page", defaultValue = "1") int page){
        List<Board> list = service.getAllPost(page);
        return ResponseEntity.ok().body(list);
    }
    @GetMapping("/api/v1/{type}/posts")
    public ResponseEntity<List> thisTypeOfPosts (@PathVariable("type") int type, @RequestParam(name = "page", defaultValue = "1") int page){
        List<Board> list = service.getAllPostsOfThatType(type, page);
        return ResponseEntity.ok().body(list);
    }
    // 게시글 작성 Post
    @GetMapping("/api/v1/management/posts/{idx}")
    public ResponseEntity<Board> getPost(@PathVariable("idx") long idx) {
        // idx 값으로 해당 게시글 찾아서 return
        Board board = service.getBoard(idx);
        return ResponseEntity.ok().body(board);
    }
    @PostMapping("/api/v1/management/posts")
    public ResponseEntity<Long> newPost (@RequestBody BoardReq board) {
        Long idx = service.newPost(board);
        return ResponseEntity.ok().body(idx);
    }
    @PutMapping("/api/v1/management/posts/{idx}")
    public ResponseEntity<Long> editPost (@PathVariable("idx") long idx, @RequestBody BoardReq boardReq){
        return ResponseEntity.ok().body(service.editBoard(idx, boardReq));
    }
    @DeleteMapping("/api/v1/management/posts")
    public ResponseEntity<String> deletePost(@RequestParam("idx") Long idx){
        // idx 로 삭제 Service Logic 처리 후 결과 return
        String result = "";
        return ResponseEntity.ok().body(result);
    }
}
