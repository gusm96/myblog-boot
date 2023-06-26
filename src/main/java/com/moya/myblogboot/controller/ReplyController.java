package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.ReplyReqDTO;
import com.moya.myblogboot.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    // 댓글 작성
    @PostMapping("/api/v1/reply")
    public ResponseEntity<String> reqToWriteReply(@RequestBody ReplyReqDTO replyReqDTO) {
        System.out.println(replyReqDTO.getWriter());
        System.out.println(replyReqDTO.getBoard_id());
        String result = replyService.writeReply(replyReqDTO);
        return ResponseEntity.ok().body(result);
    }
    // 댓글 수정

    // 댓글 삭제
    @DeleteMapping("/api/v1/reply/{replyId}")
    public ResponseEntity<Boolean> reqToDeleteReply(@PathVariable Long replyId) {
        return ResponseEntity.ok().body(replyService.deleteReply(replyId));
    }

    // 댓글 리스트
    @GetMapping("/api/v1/replies/{boardId}")
    public ResponseEntity<List> reqToReplyList(@PathVariable Long boardId) {
        return ResponseEntity.ok().body(replyService.getReplyList(boardId));
    }
}
