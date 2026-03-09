package com.moya.myblogboot.scheduler;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.dto.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import com.moya.myblogboot.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.moya.myblogboot.domain.keys.RedisKey.*;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class BoardScheduledTask {
    private final BoardRedisRepository boardRedisRepository;
    private final BoardService boardService;
    private final Lock lock = new ReentrantLock();
    private static final Long SECONDS_IN_15DAYS = 15L * 24L * 60L * 60L; // 15일

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행되도록 스케줄링
    @Transactional
    public void deleteExpiredBoards() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_IN_15DAYS);
        boardService.deletePermanently(thresholdDate);
        log.info("삭제 후 15일이 지난 게시글 영구삭제");
    }

    /**
     * 10분마다 Redis 캐시에 저장된 게시글 조회수/좋아요수를 DB에 동기화하고 캐시를 정리한다.
     * - ReentrantLock으로 자정 작업과의 동시 실행을 방지한다.
     * - 개별 게시글 단위로 에러를 처리해 하나의 실패가 전체 동기화를 중단하지 않는다.
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void updateFromRedisStoreToDB() {
        if (!lock.tryLock()) {
            log.debug("게시글 캐시 동기화 스킵: 이전 작업 진행 중");
            return;
        }
        try {
            String keyPattern = BOARD_KEY + "*";
            Set<Long> keys = boardRedisRepository.getKeys(keyPattern);
            if (keys.isEmpty()) {
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (Long boardId : keys) {
                try {
                    BoardForRedis boardForRedis = boardService.getBoardFromCache(boardId);
                    if (boardForRedis != null) {
                        updateBoard(boardForRedis);
                        deleteFromCache(boardId, boardForRedis);
                        successCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("게시글 캐시 동기화 실패 [boardId={}]: {}", boardId, e.getMessage());
                }
            }

            if (failCount == 0) {
                log.info("게시글 캐시 → DB 동기화 완료: {}건 처리", successCount);
            } else {
                log.warn("게시글 캐시 → DB 동기화 완료: 성공 {}건, 실패 {}건", successCount, failCount);
            }
        } finally {
            lock.unlock();
        }
    }

    private void updateBoard(BoardForRedis boardForRedis) {
        Board board = boardService.findById(boardForRedis.getId());
        board.updateViews(boardForRedis.totalViews());
        board.updateLikes(boardForRedis.totalLikes());
    }

    private void deleteFromCache(Long boardId, BoardForRedis boardForRedis) {
        try {
            boardRedisRepository.delete(boardForRedis);
        } catch (Exception e) {
            log.error("캐시 삭제 실패 [boardId={}]: {}", boardId, e.getMessage());
        }
    }
}