package com.moya.myblogboot.service.implementation;


import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.domain.visitor.VisitorCount;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.repository.VisitorCountRepository;
import com.moya.myblogboot.service.VisitorCountService;
import com.moya.myblogboot.utils.DateUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class VisitorCountServiceImplTest extends AbstractContainerBaseTest {

    @Autowired
    private VisitorCountService visitorCountService;

    @Autowired
    private VisitorCountRepository visitorCountRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final static String DATE_PATTERN = "yyyy-MM-dd";

    @AfterEach
    void tearDown() {
        redisTemplate.delete("visitorCount:" + getToday());
    }

    // --- incrementVisitorCount 테스트 ---

    @Test
    @DisplayName("increment — Cold Cache + 오늘/어제 DB 레코드 있음 → DB 데이터 복구 후 증가")
    void incrementVisitorCount_ColdCache_loadsFromDb() {
        // given: DB에 오늘/어제 데이터가 있고, Redis는 비어있는 상태
        String today = getToday();
        String yesterday = getPreviousDay(today);

        visitorCountRepository.save(VisitorCount.builder()
                .date(LocalDate.parse(yesterday))
                .todayVisitors(50L)
                .totalVisitors(1000L)
                .build());
        visitorCountRepository.save(VisitorCount.builder()
                .date(LocalDate.parse(today))
                .todayVisitors(20L)
                .totalVisitors(1050L)
                .build());

        redisTemplate.delete("visitorCount:" + today);

        // when
        VisitorCountDto result = visitorCountService.incrementVisitorCount(today);

        // then: DB에서 복구된 값에서 +1
        Assertions.assertThat(result.getTotal()).isEqualTo(1051L);
        Assertions.assertThat(result.getToday()).isEqualTo(21L);
        Assertions.assertThat(result.getYesterday()).isEqualTo(50L);
    }

    @Test
    @DisplayName("increment — Cold Cache + 오늘 레코드 없음 + 과거 데이터만 있음 → 최근 totalVisitors 보존")
    void incrementVisitorCount_ColdCache_noTodayRecord_preservesTotalFromRecentRecord() {
        // given: DB에 과거 날짜(2025-08-19) 데이터만 있고, 오늘/어제 레코드 없음
        String today = getToday();
        visitorCountRepository.save(VisitorCount.builder()
                .date(LocalDate.parse("2025-08-19"))
                .todayVisitors(30L)
                .totalVisitors(15420L)
                .build());

        redisTemplate.delete("visitorCount:" + today);

        // when
        VisitorCountDto result = visitorCountService.incrementVisitorCount(today);

        // then: 가장 최근 레코드의 totalVisitors(15420)에서 +1, yesterday=0 (어제 레코드 없으므로)
        Assertions.assertThat(result.getTotal()).isEqualTo(15421L);
        Assertions.assertThat(result.getToday()).isEqualTo(1L);
        Assertions.assertThat(result.getYesterday()).isEqualTo(0L);
    }

    @Test
    @DisplayName("increment — DB 완전히 비어있음 → total 0에서 시작")
    void incrementVisitorCount_ColdCache_emptyDb_startsFromZero() {
        // given: DB와 Redis 모두 비어있음
        String today = getToday();
        redisTemplate.delete("visitorCount:" + today);

        // when
        VisitorCountDto result = visitorCountService.incrementVisitorCount(today);

        // then
        Assertions.assertThat(result.getTotal()).isEqualTo(1L);
        Assertions.assertThat(result.getToday()).isEqualTo(1L);
        Assertions.assertThat(result.getYesterday()).isEqualTo(0L);
    }

    @Test
    @DisplayName("increment — Warm Cache → 단순 증가")
    void incrementVisitorCount_WarmCache_justIncrements() {
        // given: 첫 번째 increment로 Redis 키 생성
        String today = getToday();
        redisTemplate.delete("visitorCount:" + today);
        visitorCountService.incrementVisitorCount(today); // 1번째: total=1, today=1

        // when: 두 번째 increment
        VisitorCountDto result = visitorCountService.incrementVisitorCount(today);

        // then
        Assertions.assertThat(result.getTotal()).isEqualTo(2L);
        Assertions.assertThat(result.getToday()).isEqualTo(2L);
    }

    // --- getVisitorCount 테스트 ---

    @Test
    @DisplayName("getVisitorCount — 오늘 레코드 없이 과거 데이터만 있을 때 → today=0, yesterday=0, total은 최근 값")
    void getVisitorCount_noTodayRecord_returnsRecentTotal() {
        // given: DB에 과거 데이터만 있음
        String today = getToday();
        visitorCountRepository.save(VisitorCount.builder()
                .date(LocalDate.parse("2025-08-19"))
                .todayVisitors(30L)
                .totalVisitors(15420L)
                .build());

        redisTemplate.delete("visitorCount:" + today);

        // when
        VisitorCountDto result = visitorCountService.getVisitorCount(today);

        // then: yesterday=0 (어제 레코드 없음), today=0 (아직 방문자 없음), total=15420 (최근 값)
        Assertions.assertThat(result.getTotal()).isEqualTo(15420L);
        Assertions.assertThat(result.getToday()).isEqualTo(0L);
        Assertions.assertThat(result.getYesterday()).isEqualTo(0L);
    }

    @Test
    @DisplayName("getVisitorCount 후 increment → 값이 정상적으로 증가")
    void getVisitorCount_thenIncrement_valuesIncrease() {
        // given: 과거 데이터만 있는 상태에서 getVisitorCount 먼저 호출
        String today = getToday();
        visitorCountRepository.save(VisitorCount.builder()
                .date(LocalDate.parse("2025-08-19"))
                .todayVisitors(30L)
                .totalVisitors(15420L)
                .build());

        redisTemplate.delete("visitorCount:" + today);
        visitorCountService.getVisitorCount(today); // Redis 캐시 생성: {total:15420, today:0}

        // when: increment 호출
        VisitorCountDto result = visitorCountService.incrementVisitorCount(today);

        // then: getVisitorCount가 생성한 캐시 위에서 +1
        Assertions.assertThat(result.getTotal()).isEqualTo(15421L);
        Assertions.assertThat(result.getToday()).isEqualTo(1L);
    }

    // --- createTodayVisitorCount 테스트 ---

    @Test
    @DisplayName("오늘 날짜 방문자 수 생성 — 과거 데이터의 totalVisitors 이어받음")
    void createTodayVisitorCount_inheritsTotal() {
        // given
        visitorCountRepository.save(VisitorCount.builder()
                .totalVisitors(1000L)
                .todayVisitors(30L)
                .date(LocalDate.parse("2024-12-01"))
                .build());

        // when
        visitorCountService.createTodayVisitorCount();

        // then
        VisitorCount todayRecord = visitorCountRepository.findByDate(LocalDate.parse(getToday()))
                .orElseThrow();
        Assertions.assertThat(todayRecord.getTotalVisitors()).isEqualTo(1000L);
        Assertions.assertThat(todayRecord.getTodayVisitors()).isEqualTo(0L);
    }

    @Test
    @DisplayName("가장 최근의 날짜 값 가져오기")
    void findFirstByOrderByDateDesc_returnsLatest() {
        // given
        visitorCountRepository.save(VisitorCount.builder()
                .totalVisitors(500L)
                .todayVisitors(10L)
                .date(LocalDate.parse("2024-06-01"))
                .build());
        VisitorCount latest = visitorCountRepository.save(VisitorCount.builder()
                .totalVisitors(1000L)
                .todayVisitors(30L)
                .date(LocalDate.parse("2024-12-01"))
                .build());

        // when
        VisitorCount result = visitorCountRepository.findFirstByOrderByDateDesc()
                .orElseThrow();

        // then
        Assertions.assertThat(result.getTotalVisitors()).isEqualTo(latest.getTotalVisitors());
        Assertions.assertThat(result.getDate()).isEqualTo(latest.getDate());
    }

    // --- helpers ---

    private String getToday() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    private String getPreviousDay(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN))
                .minusDays(1)
                .format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }
}
