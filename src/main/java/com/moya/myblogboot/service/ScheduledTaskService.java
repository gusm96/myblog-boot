package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.repository.BoardRepository;
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
    private static final Long SECONDS_INT_15DAYS = 15L * 24L * 60L * 60L; // 15일

    // 매일 자정에 실행되도록 스케줄링
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteExpiredBoards() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_INT_15DAYS);
        boardRepository.deleteWithinPeriod(thresholdDate);
        log.info("삭제 후 15일이 지난 게시글 영구삭제");
    }

    // 1분마다 조회수/좋아요 데이터 DB에 업데이트
    @Scheduled(fixedRate = 60000) //
    @Transactional
    public void updateViewsScheduled() {
        List<Board> boards = boardRepository.findAll();
        for (Board board : boards) {
            Long currentViewsInMemory = getViews(board.getId());
            if (currentViewsInMemory == null || currentViewsInMemory < board.getViews()) {
                boardRedisRepository.setViews(board.getId(), board.getViews());
            }
            board.updateViews(getViews(board.getId()));
            board.updateLikes(getLikesCount(board.getId()));
        }
        log.info("DB 조회수 업데이트 실행");
    }

    private Long getLikesCount(Long boardId) {
        return boardRedisRepository.getLikesCount(boardId);
    }

    private Long getViews(Long boardId) {
        return boardRedisRepository.getViews(boardId);
    }
}
