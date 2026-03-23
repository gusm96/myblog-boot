package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.repository.VisitorCountRedisRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class VisitorCountRedisRepositoryImplTest extends AbstractContainerBaseTest {

    @Autowired
    private VisitorCountRedisRepository visitorCountRedisRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_DATE = "2026-03-16";
    private static final String REDIS_KEY = "visitorCount:" + TEST_DATE;

    @AfterEach
    void tearDown() {
        redisTemplate.delete(REDIS_KEY);
    }

    @Test
    @DisplayName("increment — Cold Cache(키 없음) → Optional.empty() 반환")
    void increment_ColdCache_returnsEmpty() {
        // given: 키가 없는 상태
        redisTemplate.delete(REDIS_KEY);

        // when
        Optional<VisitorCountDto> result = visitorCountRedisRepository.increment(TEST_DATE);

        // then: 키가 없으면 Optional.empty()를 반환해야 함 (DB 복구 위임 신호)
        assertThat(result).isEmpty();
        // 키가 자동 생성되지 않아야 함
        assertThat(redisTemplate.hasKey(REDIS_KEY)).isFalse();
    }

    @Test
    @DisplayName("increment — Warm Cache(키 있음) → today+1, total+1")
    void increment_WarmCache_incrementsCorrectly() {
        // given: 기존 데이터가 Redis에 있는 상태
        VisitorCountDto initial = VisitorCountDto.builder()
                .total(100L)
                .today(10L)
                .yesterday(8L)
                .build();
        visitorCountRedisRepository.save(TEST_DATE, initial);

        // when
        Optional<VisitorCountDto> result = visitorCountRedisRepository.increment(TEST_DATE);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTotal()).isEqualTo(101L);
        assertThat(result.get().getToday()).isEqualTo(11L);
        assertThat(result.get().getYesterday()).isEqualTo(8L); // yesterday는 변경되지 않아야 함
    }

    @Test
    @DisplayName("save → findByDate — 저장된 값 정확히 조회")
    void save_thenFindByDate_returnsCorrectValues() {
        // given
        VisitorCountDto dto = VisitorCountDto.builder()
                .total(5000L)
                .today(42L)
                .yesterday(37L)
                .build();

        // when
        visitorCountRedisRepository.save(TEST_DATE, dto);
        Optional<VisitorCountDto> result = visitorCountRedisRepository.findByDate(TEST_DATE);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTotal()).isEqualTo(5000L);
        assertThat(result.get().getToday()).isEqualTo(42L);
        assertThat(result.get().getYesterday()).isEqualTo(37L);
    }

    @Test
    @DisplayName("findByDate — 키 없음 → Optional.empty() 반환")
    void findByDate_noKey_returnsEmpty() {
        // given: 키가 없는 상태
        redisTemplate.delete(REDIS_KEY);

        // when
        Optional<VisitorCountDto> result = visitorCountRedisRepository.findByDate(TEST_DATE);

        // then
        assertThat(result).isEmpty();
    }
}
