package com.moya.myblogboot.service.implementation;


import com.moya.myblogboot.domain.visitor.VisitorCount;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.repository.VisitorCountRedisRepository;
import com.moya.myblogboot.repository.VisitorCountRepository;
import com.moya.myblogboot.utils.DateUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class VisitorCountServiceImplTest {

    @Autowired
    private VisitorCountRepository visitorCountRepository;

    @Autowired
    private VisitorCountRedisRepository visitorCountRedisRepository;

    private final static String DATE_PATTERN = "yyyy-MM-dd";

    /*@BeforeEach
    void before() {
        VisitorCount yesterday = VisitorCount.builder()
                .todayVisitors(5L)
                .totalVisitors(5L)
                .date(LocalDate.parse(getPreviousDay(getToday())))
                .build();

        VisitorCount today = VisitorCount.builder()
                .todayVisitors(10L)
                .totalVisitors(15L)
                .date(LocalDate.parse(getToday()))
                .build();

        visitorCountRepository.save(yesterday);
        visitorCountRepository.save(today);
    }
*/

    @Test
    @DisplayName("방문자 수 증기 및 조회")
    void 증가_조회() {
        // given
        String keyDate = getToday();
        VisitorCountDto before = getVisitorCountDto(keyDate);
        // when
        VisitorCountDto result = increment(keyDate);
        // result
        Assertions.assertThat(result.getTotal() > before.getTotal());
    }

    @Test
    @DisplayName("오늘 날짜 방문자 수 생성")
    void createTodayVisitorCount() {
        // given
        String today = getToday(); // 오늘 날짜.
        VisitorCount oldVisitorCount = visitorCountRepository.save(VisitorCount.builder()
                .totalVisitors(1000L)
                .todayVisitors(30L)
                .date(LocalDate.parse("2024-12-01"))
                .build());

        // when
        VisitorCount recentVisitorCount = retrieveRecentVisitorCount();
        VisitorCount todayVisitorCount = VisitorCount.builder()
                .totalVisitors(recentVisitorCount.getTotalVisitors())
                .todayVisitors(0L)
                .date(LocalDate.parse(getToday()))
                .build();
        VisitorCount visitorCount = visitorCountRepository.save(todayVisitorCount);
        // then
        Assertions.assertThat(visitorCount.getTotalVisitors()).isEqualTo(recentVisitorCount.getTotalVisitors());
        Assertions.assertThat(visitorCount.getTotalVisitors()).isEqualTo(oldVisitorCount.getTotalVisitors());
    }

    private VisitorCount retrieveRecentVisitorCount() {
        return visitorCountRepository.findRecentVisitorCount().orElseGet(
                () -> VisitorCount.builder()
                        .totalVisitors(0L)
                        .todayVisitors(0L)
                        .date(LocalDate.parse(DateUtil.getPreviousDay(getToday())))
                        .build());
    }

    @Test
    @DisplayName("가장 최근의 날짜 값 가져오기")
    void retrieveRecentVisitorCountTest() {
        // given
        VisitorCount oldVisitorCount = visitorCountRepository.save(VisitorCount.builder()
                .totalVisitors(1000L)
                .todayVisitors(30L)
                .date(LocalDate.parse("2024-12-01"))
                .build());
        // when
        VisitorCount recentVisitorCount = visitorCountRepository.findRecentVisitorCount().orElseGet(
                () -> VisitorCount.builder()
                        .totalVisitors(0L)
                        .todayVisitors(0L)
                        .date(LocalDate.parse(DateUtil.getPreviousDay(getToday())))
                        .build()
        );
        // then
        Assertions.assertThat(recentVisitorCount.getTotalVisitors()).isEqualTo(oldVisitorCount.getTotalVisitors());
    }

    private VisitorCountDto getVisitorCountDto(String formattedDate) {
        return visitorCountRedisRepository.findByDate(formattedDate).orElseGet(
                () -> VisitorCountDto.builder()
                        .total(0L)
                        .today(0L)
                        .yesterday(0L)
                        .build()
        );
    }

    private VisitorCountDto increment(String formattedDate) {
        // Redis에서 방문자 수 증가
        VisitorCountDto visitorCountDto = visitorCountRedisRepository.increment(formattedDate);

        // 데이터가 존재하지 않는 경우
        if (visitorCountDto == null || visitorCountDto.getTotal() == 0) {
            // DB에서 데이터 조회
            retrieveAndSaveVisitorCount(formattedDate);
            // 다시 Redis에서 증가 수행
            return visitorCountRedisRepository.increment(formattedDate);
        }
        return visitorCountDto;
    }

    private void retrieveAndSaveVisitorCount(String formattedDate) {
        visitorCountRedisRepository.save(formattedDate, retrieveVisitorCountForDB(formattedDate));
    }

    private synchronized VisitorCountDto retrieveVisitorCountForDB(String date) {
        // 1. 오늘 방문자수 조회
        VisitorCount todayVisitorCount = getVisitorDate(date);
        // 2. 어제 방문자수 조회
        VisitorCount yesterdayVisitorCount = getVisitorDate(getPreviousDay(date));

        return VisitorCountDto.builder()
                .total(todayVisitorCount.getTotalVisitors())
                .today(todayVisitorCount.getTodayVisitors())
                .yesterday(yesterdayVisitorCount.getTodayVisitors())
                .build();
    }

    private String getToday() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    private String getPreviousDay(String date) {
        LocalDateTime inputDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN)).atStartOfDay();
        LocalDateTime previousDay = inputDate.minusDays(1);
        return previousDay.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    private VisitorCount getVisitorDate(String formattedDate) {
        return visitorCountRepository.findByDate(LocalDate.parse(formattedDate)).orElseGet(
                () -> VisitorCount.of(LocalDate.parse(formattedDate), 0L, 0L));
    }
}
