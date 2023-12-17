package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.domain.member.Member;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardLikeServiceImpl implements BoardLikeService {
    private final BoardRedisRepository boardRedisRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardService boardService;
    // 게시글 좋아요 Redis
    @Override
    public Long addLikeToBoard(Long memberId, Long boardId){
        if (isBoardLiked(memberId, boardId)) {
            throw new DuplicateKeyException("이미 \"좋아요\"한 게시글 입니다.");
        }
        try {
            boardRedisRepository.addLike(boardId, memberId);
            return getBoardLikeCount(boardId);
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("게시글 \"좋아요\"를 실패했습니다.");
        }
    }

    // 게시글 좋아요 여부 체크
    @Override
    public boolean isBoardLiked(Long memberId, Long boardId) {
        if(boardRedisRepository.isMember(boardId, memberId) || boardLikeRepository.existsByBoardIdAndMemberId(boardId,memberId)){
            return true;
        }
        return false;
    }
    // 게시글 좋아요 취소 - Redis
    @Override
    public Long deleteBoardLike(Long memberId, Long boardId) {
        if (!isBoardLiked(memberId, boardId)) {
            throw new NoSuchElementException("잘못된 요청입니다.");
        }
        try {
            boardRedisRepository.likesCancel(boardId, memberId);
            return getBoardLikeCount(boardId);
        } catch (Exception e) {
            log.error("게시글 \"좋아요\" 정보 삭제 실패");
            throw new RuntimeException("게시글 \"좋아요\"취소를 실패했습니다");
        }
    }

    private Long getBoardLikeCount(Long boardId) {
        Board board = boardService.retrieveBoardById(boardId);
        return boardRedisRepository.getLikesCount(boardId) + board.getLikes();
    }
}