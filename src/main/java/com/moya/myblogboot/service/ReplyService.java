package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.Reply;
import com.moya.myblogboot.domain.ReplyReqDTO;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;

    public String writeReply(ReplyReqDTO replyReqDTO) {
        String result = "";
        Board board = boardRepository.findOne(replyReqDTO.getBoardId()).orElseThrow(() ->
                new IllegalStateException("해당 게시글은 존재하지 않습니다."));
        Reply reply = replyReqDTO.toEntity(board);
        if (replyReqDTO.getParentId() != null) {
            Reply parent = replyRepository.findOne(replyReqDTO.getBoardId());
            reply.addParentReply(parent);
            parent.addChildReply(reply);
        }
        Long saveId = replyRepository.write(reply);
        if (saveId > 0) {
            result = "success";
        } else {
            result = "failed";
        }
        return result;
    }

}
