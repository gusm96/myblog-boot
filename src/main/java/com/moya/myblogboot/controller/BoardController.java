package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BoardController {

    private BoardService service;
    @Autowired
    public BoardController(BoardService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/recent-posts")
    public ResponseEntity<List> getRecentPosts (){
        List<Board> list = service.getRecentPosts();
        return ResponseEntity.ok().body(list);
    }

    @PostMapping("/api/v1/newpost")
    public ResponseEntity<Long> newPost (@RequestBody Board board) {
        Long idx = service.newPost(board);
        return ResponseEntity.ok().body(idx);
    }
}
