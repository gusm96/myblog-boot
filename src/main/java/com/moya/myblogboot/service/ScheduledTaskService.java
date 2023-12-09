package com.moya.myblogboot.service;

import com.moya.myblogboot.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {
    private final BoardRepository boardRepository;
    private static final Long SECONDS_INT_15DAYS = 15L * 24L * 60L * 60L; // 15일

    // 매일 자정에 실행되도록 스케줄링
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void deleteExpiredBoards() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusSeconds(SECONDS_INT_15DAYS);
        boardRepository.deleteWithinPeriod(thresholdDate);
        log.info("스케줄링 실행");
    }
}
