package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.repository.BoardLikeRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.service.BoardLikeService;
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

    // 게시글 좋아요
    @Override
    public Long addLike(Long boardId, Long memberId ) {
        if(isBoardLiked(boardId, memberId)){
            throw new DuplicateKeyException("이미 \"좋아요\"한 게시글 입니다.");
        }
       return boardRedisRepository.addLike(boardId, memberId);
    }

    // 게시글 좋아요 확인
    @Override
    public boolean isBoardLiked(Long boardId ,Long memberId ) {
        return boardRedisRepository.existsMember(boardId, memberId);
    }

    // 게시글 좋아요 취소
    @Override
    public Long cancelLikes(Long boardId, Long memberId) {
        if (!isBoardLiked( boardId, memberId)) {
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

    // 게시글 좋아요 취소시 BoardLke Entity delete();
    @Async
    public void deleteBoardLikeV2(Long boardId, Long memberId) {
        Optional<BoardLike> boardLike = boardLikeRepository.findByBoardIdAndMemberId(boardId, memberId);
        if(boardLike.isPresent()) {
            boardLikeRepository.delete(boardLike.get());
        }
    }
}