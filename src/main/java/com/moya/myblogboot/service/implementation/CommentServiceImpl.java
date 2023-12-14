package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.CommentRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CommentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final AuthService authService;
    private final BoardService boardService;

    // 댓글 리스트
    @Override
    public List<CommentResDto> getCommentList(Long boardId) {
        List<Comment> comments = commentRepository.findAllByBoardId(boardId);
        return comments.stream()
                .map(parent -> CommentResDto.builder()
                        .comment(parent)
                        .child(parent.getChild().stream().map(CommentResDto::of).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

    }
    @Override
    @Transactional
    public String addComment(CommentReqDto commentReqDto, Long memberId, Long boardId) {
        Member member = authService.retrieveMemberById(memberId);
        Board board = boardService.retrieveBoardById(boardId);
        Comment comment = commentReqDto.toEntity(member, board);
        // 대댓글.
        if (commentReqDto.getParentId() != null) {
            Comment parent = retrieveCommentById(commentReqDto.getParentId());
            comment.addParentComment(parent);
            parent.addChildComment(comment);
        }
        try {
            // Comment Persist
            Comment result = commentRepository.save(comment);
            // Board Entity에 Comment 추가
            board.addComment(result);
            return "댓글이 등록되었습니다.";
        } catch (Exception e) {
            log.error("댓글 등록 중 에러 발생");
            e.printStackTrace();
            throw new RuntimeException("댓글 등록을 실패했습니다.");
        }

    }

    // 댓글 수정
    @Override
    @Transactional
    public String updateComment(Long commentId, Long memberId, String modifiedComment) {
        Comment findComment = retrieveCommentById(commentId);
        if(!findComment.getMember().getId().equals(memberId)){
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
        findComment.updateComment(modifiedComment);
        return "댓글이 수정되었습니다.";
    }

    // 댓글 삭제
    @Override
    @Transactional
    public String deleteComment(Long commentId, Long memberId) {
        Comment findComment = retrieveCommentById(commentId);
        if (!findComment.getMember().getId().equals(memberId)) {
            throw new UnauthorizedAccessException("권한이 없습니다.");
        }
        try {
            commentRepository.delete(findComment);
            return "댓글이 삭제되었습니다.";
        } catch (Exception e) {
            log.error("댓글 삭제 중 에러 발생");
            throw new RuntimeException("댓글 삭제를 실패했습니다.");
        }
    }

    // 댓글 찾기
    @Override
    public Comment retrieveCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(()
                -> new EntityNotFoundException("해당 댓글은 삭제되었거나 존재하지 않습니다."));
    }

}
