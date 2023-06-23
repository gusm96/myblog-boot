package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.ReplyReqDTO;
import com.moya.myblogboot.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    // 댓글 작성
    @PostMapping("/api/v1/reply")
    public ResponseEntity<String> writeReply(ReplyReqDTO replyReqDTO) {
        String result = replyService.writeReply(replyReqDTO);
        System.out.println(replyReqDTO.getBoardId());
        return ResponseEntity.ok().body(result);
    }
    // 댓글 수정

    // 댓글 삭제

}
