package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.repository.BoardLikeRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardCacheService;
import com.moya.myblogboot.service.BoardLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardLikeServiceImpl implements BoardLikeService {
    private final BoardRedisRepository boardRedisRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final BoardCacheService boardCacheService;
    private final AuthService authService;

    @Override
    public Long addLikes(Long boardId, Long memberId) {
        if (isLiked(boardId, memberId)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_BOARD_LIKE);
        }
        BoardForRedis board = boardCacheService.getBoardFromCache(boardId);
        addBoardLike(boardId, memberId);
        return boardRedisRepository.incrementLikes(board).totalLikes();
    }

    @Override
    public Long cancelLikes(Long boardId, Long memberId) {
        if (!isLiked(boardId, memberId)) {
            throw new EntityNotFoundException(ErrorCode.BOARD_LIKE_NOT_FOUND);
        }
        BoardForRedis board = boardCacheService.getBoardFromCache(boardId);
        deleteBoardLike(boardId, memberId);
        return boardRedisRepository.decrementLikes(board).totalLikes();
    }

    @Override
    public boolean isLiked(Long boardId, Long memberId) {
        return boardLikeRepository.existsByBoardIdAndMemberId(boardId, memberId);
    }

    void addBoardLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BOARD_NOT_FOUND));
        BoardLike boardLike = BoardLike.builder()
                .board(board)
                .member(authService.retrieve(memberId))
                .build();
        boardLikeRepository.save(boardLike);
    }

    // 게시글 좋아요 취소시 BoardLke Entity delete();
    public void deleteBoardLike(Long boardId, Long memberId) {
        Optional<BoardLike> boardLike = boardLikeRepository.findByBoardIdAndMemberId(boardId, memberId);
        if (boardLike.isPresent()) {
            boardLikeRepository.delete(boardLike.get());
        }
    }
}
