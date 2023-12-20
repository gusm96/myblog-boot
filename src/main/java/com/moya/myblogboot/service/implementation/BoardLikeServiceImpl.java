package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.BoardLikeRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.BoardLikeService;
import com.moya.myblogboot.service.BoardService;
import jakarta.persistence.EntityNotFoundException;
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
        // 메모리의 데이터를 먼저 조회한 후 없으면 DB에서 조회합니다.
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
        return boardRedisRepository.getLikesCount(boardId);
    }


    @Override
    public Long addLikeV2(Long boardId, Long memberId ) {
        if(isBoardLikedV2(boardId, memberId)){
            throw new DuplicateKeyException("이미 \"좋아요\"한 게시글 입니다.");
        }
       return boardRedisRepository.addLikeV2(boardId, memberId);
    }

    private boolean isBoardLikedV2(Long boardId ,Long memberId ) {
        return boardRedisRepository.existsMember(boardId, memberId);
    }

    @Override
    public Long cancelLikes(Long boardId, Long memberId) {
        if (!isBoardLikedV2( boardId, memberId)) {
            throw new NoSuchElementException("잘못된 요청입니다.");
        }
        try {
            deleteBoardLikeV2(boardId, memberId);
            return boardRedisRepository.deleteMembers(boardId, memberId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("게시글 좋아요 취소 실패");
            throw new RuntimeException("좋이요 취소를 실패했습니다.");
        }
    }

    @Async
    public void deleteBoardLikeV2(Long boardId, Long memberId) {
        BoardLike boardLike = boardLikeRepository.findByBoardIdAndMemberId(boardId, memberId).orElseThrow(
                () -> new EntityNotFoundException("해당 게시글 좋아요 정보를 찾지 못했습니다.")
        );
        boardLikeRepository.delete(boardLike);
    }
}