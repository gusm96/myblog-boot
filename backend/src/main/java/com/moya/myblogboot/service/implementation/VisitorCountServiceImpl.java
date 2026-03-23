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
        // 1. Redis 캐시 조회
        return visitorCountRedisRepository.findByDate(date).orElseGet(() -> {
            // 2. 캐시 미스 → DB에서 조회하여 캐시에 저장 (읽기 전용, DB 쓰기 없음)
            VisitorCountDto dto = buildVisitorCountDto(date);
            visitorCountRedisRepository.save(date, dto);
            return dto;
        });
    }

    @Override
    @Transactional
    public VisitorCountDto incrementVisitorCount(String date) {
        // Cache에서 방문자 수 증가 (키가 없으면 Optional.empty 반환)
        return visitorCountRedisRepository.increment(date).orElseGet(() -> {
            ensureTodayRecordAndCacheVisitorCount(date);
            return visitorCountRedisRepository.increment(date)
                    .orElseThrow(() -> new IllegalStateException("방문자 수 증가 실패"));
        });
    }

    @Override
    @Transactional
    public void syncVisitorCountToDb(String date) {
        VisitorCountDto dto = getVisitorCount(date);
        visitorCountRepository.save(VisitorCount.builder()
                .date(LocalDate.parse(date))
                .totalVisitors(dto.getTotal())
                .todayVisitors(dto.getToday())
                .build());
    }

    @Transactional
    @Override
    public void createTodayVisitorCount() {
        VisitorCount recentVC = visitorCountRepository.findFirstByOrderByDateDesc()
                .orElse(VisitorCount.builder()
                        .totalVisitors(0L)
                        .todayVisitors(0L)
                        .date(null)
                        .build());

        if (isTodayVisitorCount(recentVC)) {
            return;
        }

        try {
            visitorCountRepository.save(VisitorCount.builder()
                    .totalVisitors(recentVC.getTotalVisitors())
                    .todayVisitors(0L)
                    .date(LocalDate.parse(getToday()))
                    .build());
        } catch (Exception e) {
            log.error("방문자 수 생성 중 에러발생 = {}", e.getMessage());
        }
    }

    // --- private ---

    /**
     * DB에서 해당 날짜의 방문자 데이터를 조회하여 DTO를 구성한다. (읽기 전용, DB 쓰기 없음)
     * - 해당 날짜 레코드가 없으면 가장 최근 레코드의 totalVisitors를 이어받고 todayVisitors=0으로 설정한다.
     * - 어제 레코드가 없으면 yesterday=0으로 설정한다.
     */
    private VisitorCountDto buildVisitorCountDto(String date) {
        LocalDate targetDate = LocalDate.parse(date);
        LocalDate previousDate = LocalDate.parse(DateUtil.getPreviousDay(date));

        // 해당 날짜 방문자수 조회
        long total;
        long today;
        var targetOpt = visitorCountRepository.findByDate(targetDate);
        if (targetOpt.isPresent()) {
            total = targetOpt.get().getTotalVisitors();
            today = targetOpt.get().getTodayVisitors();
        } else {
            // 해당 날짜 레코드 없음 → 가장 최근 레코드의 totalVisitors를 이어받음
            total = visitorCountRepository.findFirstByOrderByDateDesc()
                    .map(VisitorCount::getTotalVisitors)
                    .orElse(0L);
            today = 0L;
        }

        // 어제 방문자수 조회 — 없으면 0
        long yesterday = visitorCountRepository.findByDate(previousDate)
                .map(VisitorCount::getTodayVisitors)
                .orElse(0L);

        return VisitorCountDto.builder()
                .total(total)
                .today(today)
                .yesterday(yesterday)
                .build();
    }

    /**
     * increment를 위한 캐시 준비: DB에 해당 날짜 레코드가 없으면 생성하고, Redis에 캐시한다.
     * DB 레코드 생성은 스케줄러가 Redis→DB 동기화 전에 Redis가 플러시될 경우 데이터 유실을 방지한다.
     */
    private synchronized void ensureTodayRecordAndCacheVisitorCount(String date) {
        LocalDate targetDate = LocalDate.parse(date);
        LocalDate previousDate = LocalDate.parse(DateUtil.getPreviousDay(date));

        // 해당 날짜 레코드 조회 — 없으면 DB에 생성 (데이터 안전을 위해)
        VisitorCount targetRecord = visitorCountRepository.findByDate(targetDate)
                .orElseGet(() -> {
                    long recentTotal = visitorCountRepository.findFirstByOrderByDateDesc()
                            .map(VisitorCount::getTotalVisitors)
                            .orElse(0L);
                    return visitorCountRepository.save(VisitorCount.of(targetDate, 0L, recentTotal));
                });

        // 어제 방문자수 조회 — 없으면 0
        long yesterday = visitorCountRepository.findByDate(previousDate)
                .map(VisitorCount::getTodayVisitors)
                .orElse(0L);

        VisitorCountDto dto = VisitorCountDto.builder()
                .total(targetRecord.getTotalVisitors())
                .today(targetRecord.getTodayVisitors())
                .yesterday(yesterday)
                .build();

        visitorCountRedisRepository.save(date, dto);
    }

    private boolean isTodayVisitorCount(VisitorCount visitorCount) {
        return visitorCount.getDate() != null &&
                visitorCount.getDate().isEqual(LocalDate.parse(getToday()));
    }
}
