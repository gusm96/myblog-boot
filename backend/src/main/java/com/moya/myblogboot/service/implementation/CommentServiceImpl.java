package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.Comment;
import com.moya.myblogboot.domain.comment.CommentReqDto;
import com.moya.myblogboot.domain.comment.CommentResDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.repository.CommentRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<CommentResDto> retrieveAll(Long boardId) {
        return commentRepository.findAllByBoardId(boardId);
    }

    @Override
    public List<CommentResDto> retrieveAllChild(Long parentId) {
        return commentRepository.findChildByParentId( parentId);
    }

    @Override
    @Transactional
    public void write(CommentReqDto commentReqDto, Long memberId, Long boardId) {
        Member member = authService.retrieve(memberId);
        Board board = boardService.findById(boardId);
        Comment comment = commentReqDto.toEntity(member, board);
        // 대댓글.
        if (commentReqDto.getParentId() != null) {
            Comment parent = retrieve(commentReqDto.getParentId());
            parent.addChildComment(comment); // 부모 엔터에 자식 등록 => 변경 감지
            comment.addParentComment(parent); // 자식 엔터티에 부모 등록
        }
        Comment result = commentRepository.save(comment);
        board.addComment(result);
    }

    // 댓글 수정
    @Override
    @Transactional
    public void update(Long commentId, Long memberId, String modifiedComment) {
        Comment findComment = retrieveWithMember(commentId);
        if (!findComment.getMember().getId().equals(memberId)) {
            throw new UnauthorizedAccessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
        findComment.updateComment(modifiedComment);
    }

    // 댓글 삭제
    @Override
    @Transactional
    public void delete(Long commentId, Long memberId) {
        Comment findComment = retrieveWithMember(commentId);
        if (!findComment.getMember().getId().equals(memberId)) {
            throw new UnauthorizedAccessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
        commentRepository.delete(findComment);
    }

    // 댓글 찾기
    @Override
    public Comment retrieve(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(()
                -> new EntityNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private Comment retrieveWithMember(Long commentId) {
        return commentRepository.findByIdWithMember(commentId).orElseThrow(()
                -> new EntityNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
    }

}
