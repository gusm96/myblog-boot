package com.moya.myblogboot.scheduler;


import com.moya.myblogboot.service.VisitorCountService;

import static com.moya.myblogboot.utils.DateUtil.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class VisitorCountScheduledTask implements ApplicationListener<ContextClosedEvent> {
    private final VisitorCountService visitorCountService;
    private final Lock lock = new ReentrantLock();

    /*
     * 데이터 정합성을 보장하기 위해 10분마다 캐시에 저장된 방문자 수를 DB에 동기화 하고, 최종적으로 자정에 방문자 수를 DB에 한번 더 동기화한다.
     * 트랜잭션은 서비스 레이어(syncVisitorCountToDb)에서 완전히 관리되므로, 락 해제 전에 커밋이 완료된다.
     * 10분마다 동기화 하는 메서드는 중복 실행되지 않도록 Lock을 사용해 제어한다.
     */

    // 자정에 Redis Store에 저장된 VisitorCount를 DB에 동기화 (전날 기준)
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void updateVisitorCountAtMidnight() {
        lock.lock();
        try {
            String yesterday = getPreviousDay(getToday());
            visitorCountService.syncVisitorCountToDb(yesterday);
            log.info("최종 방문자 수 DB 업데이트 완료 : {}", yesterday);
        } catch (Exception e) {
            log.error("방문자 수 업데이트 중 오류발생 : {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // 10분마다 Redis Store에 저장된 VisitorCount를 DB에 동기화
    // fixedDelay: 이전 실행 완료 후 10분 뒤 다음 실행 (fixedRate와 달리 중첩 실행 구조적 방지)
    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    public void updateVisitorCountEveryTenMinutes() {
        if (lock.tryLock()) {
            try {
                String today = getToday();
                visitorCountService.syncVisitorCountToDb(today);
                log.info("방문자 수 DB 업데이트 완료 : {}", getTodayAndTime());
            } catch (Exception e) {
                log.error("방문자 수 업데이트 중 오류발생 : {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }

    // 자정 1분 후 오늘 날짜 엔티티 생성 (updateVisitorCountAtMidnight 완료 후 실행 보장)
    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Seoul")
    public void createTodayVisitorCountAtMidnight() {
        visitorCountService.createTodayVisitorCount();
    }

    // 정상 종료(SIGTERM) 시 Redis → DB 즉시 동기화
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        lock.lock();
        try {
            String today = getToday();
            visitorCountService.syncVisitorCountToDb(today);
            log.info("Shutdown: Redis → DB 동기화 완료 ({})", today);
        } catch (Exception e) {
            log.error("Shutdown: 동기화 실패 — AOF 복원에 의존. 원인: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
