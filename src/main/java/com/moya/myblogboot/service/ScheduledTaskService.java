package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.board.BoardForRedis;
import com.moya.myblogboot.repository.BoardRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static com.moya.myblogboot.domain.keys.RedisKey.*;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final BoardRedisRepository boardRedisRepository;
    private final BoardService boardService;
    private static final Long SECONDS_INT_15DAYS = 15L * 24L * 60L * 60L; // 15일

    @Scheduled(cron = "0 0 0 * * ?")// 매일 자정에 실행되도록 스케줄링
    @Transactional
    public void deleteExpiredBoards() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_INT_15DAYS);
        boardService.deletePermanently(thresholdDate);
        log.info("삭제 후 15일이 지난 게시글 영구삭제");
    }

    @Scheduled(fixedRate = 600000) // 10분마다 DB 갱싱 및 캐시 정리
    @Transactional
    public void updateFromRedisStoreToDB() {
        String keyPattern = BOARD_KEY + "*";
        Set<Long> keys = boardRedisRepository.getKeys(keyPattern);
        try{
            for (Long key : keys) {
                // 메모리에서 데이터 조회
                BoardForRedis boardForRedis = boardService.retrieveBoardInRedisStore(key);
                if (boardForRedis != null) {
                    // 수정할 대상 게시글 엔터티 조회
                    updateBoards(boardForRedis);
                    deleteFromCache(boardForRedis);
                }
            }
            log.info("Updated from Cache to DB");
        }catch (Exception e){
            log.error(e.getMessage());
            throw new RuntimeException("Failed To Update from Cache to DB");
        }
    }

    // 게시글 업데이트
    private void updateBoards(BoardForRedis boardForRedis) {
        Board findBoard = boardService.retrieve(boardForRedis.getId());
        // 조회수 업데이트
        findBoard.updateViews(boardForRedis.totalViews());
        // 좋아요수 업데이트
        findBoard.updateLikes(boardForRedis.totalLikes());
    }

    // 캐시 데이터 삭제
    private void deleteFromCache (BoardForRedis boardForRedis){
        try {
         boardRedisRepository.delete(boardForRedis);
        }catch (Exception e){
            throw new RuntimeException("Failed To delete from Cache");
        }
    }
}