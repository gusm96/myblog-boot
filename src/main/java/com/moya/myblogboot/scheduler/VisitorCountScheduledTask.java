package com.moya.myblogboot.scheduler;


import com.moya.myblogboot.domain.visitor.VisitorCount;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.repository.VisitorCountRepository;
import com.moya.myblogboot.service.VisitorCountService;

import static com.moya.myblogboot.utils.DateUtil.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Profile("!test")
@Service
@RequiredArgsConstructor
public class VisitorCountScheduledTask {
    private final VisitorCountService visitorCountService;
    private final VisitorCountRepository visitorCountRepository;
    private final Lock lock = new ReentrantLock();

    /*
     * 데이터 정합성을 보장하기 위해 10분마다 캐시에 저장된 방문자 수를 DB에 동기화 하고, 최종적으로 자정에 방문자 수를 DB에 한번 더 동기화한다.
     * 이때, 10분마다 동기화 하는 메서드는 중복 실행되지 않도록 하기 위해 Lock 클래스를 사용해 제어한다.
     */
    // 자정에 Redis Store에 저장된 VisitorCount를 DB에 동기화.
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void updateVisitorCountAtMidnight() {
        lock.lock();
        try {
            // Redis Store에서 전날의 값으로 데이터 가져온다.
            String today = getToday();
            String yesterday = getPreviousDay(today);
            VisitorCountDto previousVisitorCount = visitorCountService.getVisitorCount(yesterday);
            VisitorCount visitorCount = VisitorCount.builder()
                    .date(LocalDate.parse(yesterday))
                    .totalVisitors(previousVisitorCount.getTotal())
                    .todayVisitors(previousVisitorCount.getToday())
                    .build();
            visitorCountRepository.save(visitorCount);
            log.info("최종 방문자 수 DB 업데이트 완료 : {}", yesterday);
        } catch (Exception e) {
            log.error("방문자 수 업데이트 중 오류발생 : {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    // 10분마다 Redis Store에 저장된 VisitorCount를 DB에 동기화
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void updateVisitorCountEveryTenMinutes() {
        if (lock.tryLock()) {
            try {
                String today = getToday();
                VisitorCountDto visitorCountDto = visitorCountService.getVisitorCount(today);
                VisitorCount visitorCount = VisitorCount.builder()
                        .totalVisitors(visitorCountDto.getTotal())
                        .todayVisitors(visitorCountDto.getToday())
                        .date(LocalDate.parse(today))
                        .build();
                visitorCountRepository.save(visitorCount);
                log.info("방문자 수 DB 업데이트 완료 : {}", today);
            } catch (Exception e) {
                log.error("방문자 수 업데이트 중 오류발생 : {}", e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }
}
