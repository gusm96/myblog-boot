package com.moya.myblogboot.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorHmacServiceImplTest {

    private static final String TEST_SECRET = "test-secret-key-32bytes-minimum!!";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private VisitorHmacServiceImpl hmacService;

    @BeforeEach
    void setUp() {
        hmacService = new VisitorHmacServiceImpl(TEST_SECRET);
    }

    @Test
    @DisplayName("generateToken — {date}:{uuid}:{signature} 형식, date == 오늘(KST)")
    void generateToken_형식_검증() {
        String token = hmacService.generateToken();

        // lastIndexOf로 payload와 signature 분리
        int lastColon = token.lastIndexOf(":");
        assertThat(lastColon).isGreaterThan(0);

        String payload = token.substring(0, lastColon);
        String signature = token.substring(lastColon + 1);

        String[] payloadParts = payload.split(":", 2);
        assertThat(payloadParts).hasSize(2);

        String todayKst = ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(payloadParts[0]).isEqualTo(todayKst);
        assertThat(payloadParts[1]).isNotBlank(); // UUID
        assertThat(signature).isNotBlank();
    }

    @Test
    @DisplayName("generateToken — 호출할 때마다 고유한 토큰 생성 (사용자 구분)")
    void generateToken_고유성_검증() {
        String token1 = hmacService.generateToken();
        String token2 = hmacService.generateToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("isValid — 오늘 날짜 + 올바른 서명 → true")
    void isValid_정상_토큰() {
        String token = hmacService.generateToken();
        assertThat(hmacService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid — 어제 날짜 토큰 → false")
    void isValid_날짜_만료() {
        String yesterday = ZonedDateTime.now(KST).minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String todayToken = hmacService.generateToken();
        // date:uuid:signature → 날짜만 어제로 교체 (서명 불일치로 false)
        int firstColon = todayToken.indexOf(":");
        String expiredToken = yesterday + todayToken.substring(firstColon);

        assertThat(hmacService.isValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("isValid — 날짜는 오늘, 서명 위변조 → false")
    void isValid_서명_위변조() {
        String today = ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String tamperedToken = today + ":tampered-signature-value";

        assertThat(hmacService.isValid(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("isValid — null 입력 → false")
    void isValid_null() {
        assertThat(hmacService.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isValid — 빈 문자열 → false")
    void isValid_빈값() {
        assertThat(hmacService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid — 구분자 없음 → false")
    void isValid_구분자_없음() {
        assertThat(hmacService.isValid("2026-03-16nosignature")).isFalse();
    }

    @Test
    @DisplayName("isValid — 콜론만 있는 값 ':' → false")
    void isValid_콜론만() {
        assertThat(hmacService.isValid(":")).isFalse();
    }

    @Test
    @DisplayName("secondsUntilMidnight — 0 초과 86400 이하")
    void secondsUntilMidnight_범위() {
        int seconds = hmacService.secondsUntilMidnight();
        assertThat(seconds).isGreaterThan(0).isLessThanOrEqualTo(86400);
    }
}
