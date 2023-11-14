package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.UnauthorizedAccessException;
import com.moya.myblogboot.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;

    @Transactional
    public String addComment(Member member, Board board, CommentReqDto commentReqDto) {
        Comment comment = commentReqDto.toEntity(member, board);
        // 대댓글.
        if (commentReqDto.getParentId() != null) {
            Comment parent = retrieveCommentById(commentReqDto.getParentId());
            comment.addParentComment(parent);
            parent.addChildComment(comment);
        }
        try {
            Long result = commentRepository.write(comment);
            // Board Entity에 Comment 추가 ( 변경감지 )
            board.addComment(comment);
            if (result > 0) {
                return "댓글이 등록되었습니다.";
            }
            return ("댓글 등록울 실패했습니다.");
        } catch (Exception e) {
            log.error("댓글 등록 중 에러 발생");
            throw new RuntimeException("댓글 등록을 실패했습니다.");
        }
        
    }
    // 댓글 수정
    @Transactional
    public String updateComment(Long memberId, Long commentId, String comment) {
        Comment findComment = retrieveCommentById(commentId);
        if(findComment.getMember().getId() != memberId){
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
        findComment.updateComment(comment);
        return "result";
    }
    // 댓글 삭제
    @Transactional
    public boolean deleteComment(Long memberId, Long commentId, Board board) {
        Comment findComment = retrieveCommentById(commentId);
        if(findComment.getMember().getId() != memberId) {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
        try {
            board.removeComment(findComment);
            commentRepository.removeComment(findComment);
            return true;
        } catch (Exception e) {
            log.error("댓글 삭제 중 에러 발생");
            throw new RuntimeException("댓글 삭제를 실패했습니다.");
            }
    }
    // 댓글 리스트
    public List<CommentResDto> getCommentList(Long boardId) {
        List<Comment> list = commentRepository.commentList(boardId);
        return list.stream()
                .filter(comment -> comment.getParent() == null) // 부모 댓글만 선택
                .map(CommentResDto::of) // CommentResDto로 변환
                .collect(Collectors.toList());
    }
    
    // 댓글 찾기
    public Comment retrieveCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(()
                -> new EntityNotFoundException("해당 댓글은 삭제되었거나 존재하지 않습니다."));
    }


}
