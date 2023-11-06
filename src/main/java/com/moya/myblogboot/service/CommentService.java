package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDTO;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final BoardService boardService;
    @Transactional
    public String writeComment(CommentReqDTO commentReqDTO) {
        Board board = boardService.retrieveBoardById(commentReqDTO.getBoard_id());
        Comment comment = commentReqDTO.toEntity(board);
        if (commentReqDTO.getParent_id() != null) {
            Comment parent = findComment(commentReqDTO.getParent_id());
            comment.addParentComment(parent);
            parent.addChildComment(comment);
        }
        Long saveId = commentRepository.write(comment);
        board.getComments().add(comment);
        if (saveId > 0) {
            return "댓글이 등록되었습니다.";
        } else {
            return "댓글 등록에 실패하였습니다.";
        }
    }

    @Transactional
    public boolean deleteComment(Long commentId) {
        try {
            commentRepository.removeComment(commentId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<CommentResDto> getCommentList(Long boardId) {
        List<Comment> list = commentRepository.commentList(boardId);
        List<CommentResDto> result = list.stream().map(CommentResDto::of).toList();
        return result;
    }

    public Comment findComment(Long commentId) {
        return commentRepository.findOne(commentId).orElseThrow(()
                -> new IllegalStateException("해당 댓글은 삭제되었거나 존재하지 않습니다."));
    }

}
