package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.visitor.VisitorCount;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.repository.VisitorCountRedisRepository;
import com.moya.myblogboot.repository.VisitorCountRepository;
import com.moya.myblogboot.service.VisitorCountService;
import com.moya.myblogboot.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static com.moya.myblogboot.utils.DateUtil.getToday;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitorCountServiceImpl implements VisitorCountService {
    private final VisitorCountRepository visitorCountRepository;
    private final VisitorCountRedisRepository visitorCountRedisRepository;

    @Override
    public VisitorCountDto getVisitorCount(String date) {
        // 매개변수로 주어지는 날짜로 Redis Store에서 값을 찾는다.
        return visitorCountRedisRepository.findByDate(date).orElseGet(
                () -> retrieveAndSaveRecentVisitorCount() // 값이 없는 경우 DB에서 해당 날짜의 값을 찾고 Redis Store에 저장한다.
        );
    }

    @Override
    @Transactional
    public VisitorCountDto incrementVisitorCount(String date) {
        // Redis에서 방문자 수 증가
        VisitorCountDto visitorCountDto = visitorCountRedisRepository.increment(date);
        // 데이터가 존재하지 않는 경우
        if (visitorCountDto == null || visitorCountDto.getTotal() < 0) {
            retrieveAndSaveVisitorCount(date);
            // 다시 Redis에서 증가 수행
            return visitorCountRedisRepository.increment(date);
        }
        return visitorCountDto;
    }

    // DB에서 VisitorCount 조회 후 Redis Store에 저장.
    private void retrieveAndSaveVisitorCount(String date) {
        visitorCountRedisRepository.save(date, retrieveVisitorCountDto(date));
    }

    private synchronized VisitorCountDto retrieveVisitorCountDto(String date) {
        // 1. 오늘 방문자수 조회
        VisitorCount todayVisitorCount = retrieveVisitorCountFromDB(date);
        // 2. 어제 방문자수 조회
        VisitorCount yesterdayVisitorCount = retrieveVisitorCountFromDB(DateUtil.getPreviousDay(date));

        return VisitorCountDto.builder()
                .total(todayVisitorCount.getTotalVisitors())
                .today(todayVisitorCount.getTodayVisitors())
                .yesterday(yesterdayVisitorCount.getTodayVisitors())
                .build();
    }

    // DB 에서 VisitorCount 조회
    @Transactional
    public VisitorCount retrieveVisitorCountFromDB(String formattedDate) {
        LocalDate date;
        try {
            date = LocalDate.parse(formattedDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식이 잘못되었습니다.");
        }
        return visitorCountRepository.findByDate(date).orElseGet(
                () -> visitorCountRepository.save(VisitorCount.of(LocalDate.parse(formattedDate), 0L, 0L)));
    }

    @Transactional
    @Override
    public void createTodayVisitorCount() {
        VisitorCount recentVC = retrieveRecentVisitorCount();

        // 최근의 값이 오늘의 VisitorCount라면 생성하지 않는다.
        if (isTodayVisitorCount(recentVC)) {
            return;
        }
        createNewVisitorCount(recentVC);
    }

    private boolean isTodayVisitorCount(VisitorCount visitorCount) {
        return visitorCount.getDate() != null &&
                visitorCount.getDate().isEqual(LocalDate.parse(getToday()));
    }

    private VisitorCount createNewVisitorCount(VisitorCount recentVC) {
        VisitorCount todayVC = VisitorCount.builder()
                .totalVisitors(recentVC.getTotalVisitors())
                .todayVisitors(0L)
                .date(LocalDate.parse(getToday()))
                .build();

        try {
            return visitorCountRepository.save(todayVC);
        } catch (Exception e) {
            log.error("방문자 수 생성 중 에러발생 = {}", e.getMessage());
            return VisitorCount.builder().build(); // 에러 발생 시 빈 객체 반환
        }
    }

    private VisitorCountDto retrieveAndSaveRecentVisitorCount() {
        VisitorCount visitorCount = retrieveRecentVisitorCount();

        VisitorCountDto visitorCountDto = VisitorCountDto.builder()
                .total(visitorCount.getTotalVisitors())
                .today(0L)
                .yesterday(visitorCount.getTodayVisitors())
                .build();

        visitorCountRedisRepository.save(getToday(), visitorCountDto);

        return visitorCountDto;
    }

    private VisitorCount retrieveRecentVisitorCount() {
        return visitorCountRepository.findRecentVisitorCount().orElseGet(
                () -> VisitorCount.builder()
                        .totalVisitors(0L)
                        .todayVisitors(0L)
                        .date(null)
                        .build()
        );
    }
}
