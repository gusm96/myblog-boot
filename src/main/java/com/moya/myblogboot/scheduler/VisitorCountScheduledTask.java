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

@Slf4j
@Profile("!test")
@Service
@RequiredArgsConstructor
public class VisitorCountScheduledTask {
    private final VisitorCountService visitorCountService;
    private final VisitorCountRepository visitorCountRepository;

    // 00:30분에 Redis Store에 저장된 VisitorCount를 DB에 저장하고 초기화 하는 작업을 수행.
    @Scheduled(cron = "0 30 0 * * ?") // 매일 오전 00:30에 실행
    @Transactional
    public void visitorCountSaveToDB() {
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
        log.info("방문자 수 DB 업데이트 완료");
    }

    @Scheduled(fixedRate = 60000)
    public void visitorCountSave(){
        String today = getToday();
        VisitorCountDto visitorCountDto = visitorCountService.getVisitorCount(today);

    }
}
