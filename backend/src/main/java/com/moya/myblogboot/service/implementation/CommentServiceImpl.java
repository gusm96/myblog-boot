package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.comment.*;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.UnauthorizedAccessException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.CommentRepository;
import com.moya.myblogboot.service.BoardService;
import com.moya.myblogboot.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final BoardService boardService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<CommentResDto> retrieveAll(Long boardId) {
        return commentRepository.findAllByBoardId(boardId);
    }

    @Override
    public List<CommentResDto> retrieveAllChild(Long parentId) {
        return commentRepository.findChildByParentId(parentId);
    }

    @Override
    @Transactional
    public CommentWriteResDto write(CommentReqDto reqDto, Long boardId, boolean isAdmin) {
        Board board = boardService.findById(boardId);

        String nickname;
        String discriminator;
        String password;

        if (isAdmin) {
            nickname = "[관리자]";
            discriminator = "0000";
            password = "";
        } else {
            if (reqDto.getNickname() == null || reqDto.getNickname().isBlank())
                throw new UnauthorizedException(ErrorCode.INVALID_INPUT);
            if (reqDto.getPassword() == null || reqDto.getPassword().isBlank())
                throw new UnauthorizedException(ErrorCode.INVALID_INPUT);
            nickname = reqDto.getNickname();
            discriminator = generateDiscriminator(boardId, nickname);
            password = passwordEncoder.encode(reqDto.getPassword());
        }

        Comment comment = Comment.builder()
                .comment(reqDto.getComment())
                .nickname(nickname)
                .discriminator(discriminator)
                .password(password)
                .isAdmin(isAdmin)
                .board(board)
                .build();

        if (reqDto.getParentId() != null) {
            Comment parent = retrieve(reqDto.getParentId());
            parent.addChildComment(comment);
            comment.addParentComment(parent);
        }

        Comment result = commentRepository.save(comment);
        board.addComment(result);

        return CommentWriteResDto.builder()
                .nickname(nickname)
                .discriminator(discriminator)
                .build();
    }

    @Override
    @Transactional
    public void update(Long commentId, CommentUpdateReqDto reqDto, boolean isAdmin) {
        Comment comment = retrieve(commentId);
        if (!isAdmin) {
            if (reqDto.getPassword() == null
                    || !passwordEncoder.matches(reqDto.getPassword(), comment.getPassword())) {
                throw new UnauthorizedAccessException(ErrorCode.COMMENT_ACCESS_DENIED);
            }
        }
        comment.updateComment(reqDto.getComment());
    }

    @Override
    @Transactional
    public void delete(Long commentId, CommentDeleteReqDto reqDto, boolean isAdmin) {
        Comment comment = retrieve(commentId);
        if (!isAdmin) {
            if (reqDto.getPassword() == null
                    || !passwordEncoder.matches(reqDto.getPassword(), comment.getPassword())) {
                throw new UnauthorizedAccessException(ErrorCode.COMMENT_ACCESS_DENIED);
            }
        }
        commentRepository.delete(comment);
    }

    @Override
    public Comment retrieve(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /** 게시글 범위 내에서 nickname+discriminator 중복 방지 */
    private String generateDiscriminator(Long boardId, String nickname) {
        for (int i = 0; i < 10; i++) {
            String discriminator = String.format("%04d",
                    ThreadLocalRandom.current().nextInt(1000, 10000));
            if (!commentRepository.existsByBoard_IdAndNicknameAndDiscriminator(
                    boardId, nickname, discriminator)) {
                return discriminator;
            }
        }
        // 매우 드문 경우: 시도 초과 시 밀리초 기반 fallback
        return String.valueOf(System.currentTimeMillis() % 9000 + 1000);
    }
}
