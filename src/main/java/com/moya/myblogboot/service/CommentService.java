package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;
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

    @Transactional
    public String writeComment(Member member, Board board, CommentReqDto commentReqDto) {
        Comment comment = commentReqDto.toEntity(member, board);
        // 대댓글.
        if (commentReqDto.getParent_id() != null) {
            Comment parent = retrieveCommentById(commentReqDto.getParent_id());
            comment.addParentComment(parent);
            parent.addChildComment(comment);
        }
        Long result = commentRepository.write(comment);
        // Board Entity에 Comment 추가 ( 변경감지 )
        board.addComment(comment);

        if (result > 0) {
            return "댓글이 등록되었습니다.";
        } else {
            throw new RuntimeException("댓글 작성을 실패했습니다.");
        }
    }

    @Transactional
    public boolean deleteComment(Long memberId, Long commentId, Board board) {
        Comment findComment = retrieveCommentById(commentId);
        if(findComment.getMember().getId() != memberId) {
            return false;
        }
        try {
            board.removeComment(findComment);
            commentRepository.removeComment(findComment);
            return true;
        } catch (Exception e) {
                throw new RuntimeException("댓글 삭제를 실패했습니다.");
            }
    }

    public List<CommentResDto> getCommentList(Long boardId) {
        List<Comment> list = commentRepository.commentList(boardId);
        return list.stream().map(CommentResDto::of).toList();

    }


    public Comment retrieveCommentById(Long commentId) {
        return commentRepository.findOne(commentId).orElseThrow(()
                -> new IllegalStateException("해당 댓글은 삭제되었거나 존재하지 않습니다."));
    }

}
