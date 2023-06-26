package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Board;
import com.moya.myblogboot.domain.Reply;
import com.moya.myblogboot.domain.ReplyReqDTO;
import com.moya.myblogboot.domain.ReplyResDto;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final BoardService boardService;
    @Transactional
    public String writeReply(ReplyReqDTO replyReqDTO) {
        Board board = boardService.findBoard(replyReqDTO.getBoard_id());
        Reply reply = replyReqDTO.toEntity(board);
        if (replyReqDTO.getParent_id() != null) {
            Reply parent = findReply(replyReqDTO.getParent_id());
            reply.addParentReply(parent);
            parent.addChildReply(reply);
        }
        Long saveId = replyRepository.write(reply);
        board.getReplies().add(reply);
        if (saveId > 0) {
            return "success";
        } else {
            return "failed";
        }
    }

    @Transactional
    public boolean deleteReply(Long replyId) {
        try {
            replyRepository.removeReply(replyId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ReplyResDto> getReplyList(Long boardId) {
        List<Reply> list = replyRepository.replyList(boardId);
        List<ReplyResDto> result = list.stream().map(ReplyResDto::of).toList();
        return result;
    }

    public Reply findReply(Long replyId) {
        return replyRepository.findOne(replyId).orElseThrow(()
                -> new IllegalStateException("해당 댓글은 삭제되었거나 존재하지 않습니다."));
    }

}
