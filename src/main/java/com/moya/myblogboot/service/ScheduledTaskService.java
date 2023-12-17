package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardLike;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.BoardLikeRepository;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final BoardRepository boardRepository;
    private final BoardRedisRepository boardRedisRepository;
    private final MemberRepository memberRepository;
    private final BoardLikeRepository boardLikeRepository;
    private static final Long SECONDS_INT_15DAYS = 15L * 24L * 60L * 60L; // 15일
    private static final String BOARD_LIKES_KEY = "likes:";
    private static final String BOARD_VIEWS_KEY = "views:";

    // 매일 자정에 실행되도록 스케줄링
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteExpiredBoards() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_INT_15DAYS);
        boardRepository.deleteWithinPeriod(thresholdDate);
        log.info("삭제 후 15일이 지난 게시글 영구삭제");
    }

    // 10분마다 조회수 데이터 DB에 업데이트
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateViews() {
        // 메모리에 존재하는 데이터만 업데이트
        List<Long> keys = getKeys(BOARD_VIEWS_KEY);

        keys.forEach(this::updateViewsForBoard);
        log.info("DB 게시글 조회수 업데이트");
    }

    private void updateViewsForBoard(Long boardId) {
        // 게시글 조회
        Board board = getBoard(boardId);
        // 메모리에 저장된 현재 views 조회
        Long currentViewsInMemory = getViews(board.getId());
        // 메모리의 값이 null이 아니고 DB의 값보다 크다면 업데이트
        if (currentViewsInMemory != null && currentViewsInMemory > board.getViews()) {
            board.updateViews(currentViewsInMemory);
        }
        // 메모리에서 데이터 삭제
        boardRedisRepository.deleteViews(board.getId());
    }

    // 10분마다 좋아요 수 데이터 DB에 업데이트
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateLikes() {
        // 메모리에 존재하는 데이터만 업데이트
        List<Long> keys = getKeys(BOARD_LIKES_KEY);
        for (Long boardId : keys) {
            updateLikesForBoard(boardId);
        }
        log.info("DB 게시글 좋아요 수 업데이트");
    }

    private void updateLikesForBoard(Long boardId) {
        // 해당 boardId를 key로 가지는 value값인 memberId를 모두 찾아온다.
        List<Long> membersId = boardRedisRepository.getLikesMembers(boardId);
        Board board = getBoard(boardId);
        // 메모리에 저장된 데이터를 BoardLike Entity로 DB에 저장
        saveBoardLikes(board, membersId);
        // BoardEntity의 총 좋아요 개수 수정
        updateBoardLikesCount(board);
        // 메모리 데이터 삭제
        boardRedisRepository.deleteLikes(board.getId());
    }

    private void saveBoardLikes(Board board, List<Long> membersId) {
        for (Long memberId : membersId) {
            Member member = getMember(memberId);
            BoardLike boardLike = BoardLike.builder().board(board).member(member).build();
            boardLikeRepository.save(boardLike);
        }
    }
    private void updateBoardLikesCount(Board board) {
        board.updateLikes((long) board.getBoardLikes().size());
    }

    private List<Long> getKeys(String key) {
        return boardRedisRepository.getKeysValues(key);
    }

    private Long getViews(Long boardId) {
        return boardRedisRepository.getViews(boardId);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new EntityNotFoundException("회원이 존재하지 않습니다")
        );
    }

    private Board getBoard(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(
                () -> new EntityNotFoundException("게시글이 존재하지 않습니다.")
        );
    }
}