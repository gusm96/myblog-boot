package com.moya.myblogboot.controller;

import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.utils.PrincipalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class DeletedBoardController {

    private final BoardService boardService;

    @GetMapping("/api/v1/deleted-boards")
    public ResponseEntity<?> getDeletedBoards(@RequestParam(name = "p", defaultValue = "1") int page) {
        return ResponseEntity.ok().body(boardService.retrieveAllDeleted(page - 1));
    }

    @PutMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> cancelDeletedBoard(@PathVariable("boardId") Long boardId,
                                                Principal principal) {
        Long memberId = PrincipalUtil.getMemberId(principal);
        boardService.undelete(boardId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/api/v1/deleted-boards/{boardId}")
    public ResponseEntity<?> deleteBoardPermanently(@PathVariable("boardId") Long boardId) {
        boardService.deletePermanently(boardId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
