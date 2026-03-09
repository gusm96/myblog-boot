package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.repository.BoardLikeRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardLikeServiceImpl implements BoardLikeService {
    private final BoardRedisRepository boardRedisRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardService boardService;
    private final AuthService authService;

    @Override
    public Long addLikes(Long boardId, Long memberId) {
        if (isLiked(boardId, memberId)) {
            throw new DuplicateKeyException("이미 \"좋아요\"한 게시글 입니다.");
        }
        BoardForRedis board = boardService.getBoardFromCache(boardId);
        try {
            addBoardLike(boardId, memberId);
            return boardRedisRepository.incrementLikes(board).totalLikes();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요\"를 실패했습니다.");
        }
    }

    @Override
    public Long cancelLikes(Long boardId, Long memberId) {
        if (!isLiked(boardId, memberId)) {
            throw new NoSuchElementException("잘못된 요청입니다.");
        }
        BoardForRedis board = boardService.getBoardFromCache(boardId);
        try {
            deleteBoardLike(boardId, memberId);
            return boardRedisRepository.decrementLikes(board).totalLikes();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요 취소\"를 실패했습니다.");
        }
    }

    @Override
    public boolean isLiked(Long boardId, Long memberId) {
        return boardLikeRepository.existsByBoardIdAndMemberId(boardId, memberId);
    }

    @Async
    void addBoardLike(Long boardId, Long memberId) {
        BoardLike boardLike = BoardLike.builder()
                .board(boardService.findById(boardId))
                .member(authService.retrieve(memberId))
                .build();
        boardLikeRepository.save(boardLike);
    }

    // 게시글 좋아요 취소시 BoardLke Entity delete();
    @Async
    public void deleteBoardLike(Long boardId, Long memberId) {
        Optional<BoardLike> boardLike = boardLikeRepository.findByBoardIdAndMemberId(boardId, memberId);
        if (boardLike.isPresent()) {
            boardLikeRepository.delete(boardLike.get());
        }
    }
}