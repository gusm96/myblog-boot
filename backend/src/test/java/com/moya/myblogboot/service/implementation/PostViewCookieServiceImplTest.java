package com.moya.myblogboot.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class PostViewCookieServiceImplTest {

    // 쿠키 형식: {date}:{postId1}_{postId2}_...|{HMAC_SIGNATURE}

    private static final String TEST_SECRET = "test-board-view-hmac-secret-key-32b!";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private PostViewCookieServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostViewCookieServiceImpl(TEST_SECRET);
    }

    // ──────────────────────────────────────────────
    // addViewed
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("addViewed — 최초 생성 (cookieValue == null): {date}:{postId}|{sig}")
    void addViewed_최초생성() {
        Long postId = 42L;
        String result = service.addViewed(null, postId);

        String today = todayKst();
        int lastPipe = result.lastIndexOf('|');
        assertThat(lastPipe).isGreaterThan(0);

        String payload = result.substring(0, lastPipe);
        assertThat(payload).isEqualTo(today + ":" + postId);

        // 서명이 비어있지 않아야 함
        assertThat(result.substring(lastPipe + 1)).isNotBlank();
    }

    @Test
    @DisplayName("addViewed — 기존 쿠키에 새 postId 추가: 기존 ids 뒤에 _{postId} 추가")
    void addViewed_기존에_추가() {
        Long first = 42L;
        Long second = 108L;

        String firstCookie = service.addViewed(null, first);
        String secondCookie = service.addViewed(firstCookie, second);

        int lastPipe = secondCookie.lastIndexOf('|');
        String payload = secondCookie.substring(0, lastPipe);
        assertThat(payload).isEqualTo(todayKst() + ":" + first + "_" + second);
    }

    @Test
    @DisplayName("addViewed — 세 번 추가해도 날짜 섹션은 하나만 존재")
    void addViewed_날짜_섹션_하나() {
        String cookie = service.addViewed(null, 1L);
        cookie = service.addViewed(cookie, 2L);
        cookie = service.addViewed(cookie, 3L);

        int lastPipe = cookie.lastIndexOf('|');
        String payload = cookie.substring(0, lastPipe);

        assertThat(payload).startsWith(todayKst() + ":");
        assertThat(payload.indexOf(':', todayKst().length() + 1)).isEqualTo(-1);
    }

    // ──────────────────────────────────────────────
    // isValid
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("isValid — 오늘 날짜 + 올바른 서명 → true")
    void isValid_정상() {
        String cookie = service.addViewed(null, 42L);
        assertThat(service.isValid(cookie)).isTrue();
    }

    @Test
    @DisplayName("isValid — null → false")
    void isValid_null() {
        assertThat(service.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isValid — 빈 문자열 → false")
    void isValid_빈값() {
        assertThat(service.isValid("")).isFalse();
    }

    @Test
    @DisplayName("isValid — '|' 없음 → false")
    void isValid_파이프_없음() {
        assertThat(service.isValid("2026-03-24:42nosignature")).isFalse();
    }

    @Test
    @DisplayName("isValid — 어제 날짜 → false (날짜 만료)")
    void isValid_날짜_만료() {
        String yesterday = ZonedDateTime.now(KST).minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String todayCookie = service.addViewed(null, 42L);

        int colonIdx = todayCookie.indexOf(':');
        String expiredCookie = yesterday + todayCookie.substring(colonIdx);
        assertThat(service.isValid(expiredCookie)).isFalse();
    }

    @Test
    @DisplayName("isValid — 서명 위변조 → false")
    void isValid_서명_위변조() {
        String tampered = todayKst() + ":42|tampered-signature-value";
        assertThat(service.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isValid — 다른 secret으로 서명된 쿠키 → false")
    void isValid_다른_시크릿() {
        PostViewCookieServiceImpl otherService =
                new PostViewCookieServiceImpl("other-secret-key-32bytes-minimum!!");
        String otherCookie = otherService.addViewed(null, 42L);
        assertThat(service.isValid(otherCookie)).isFalse();
    }

    // ──────────────────────────────────────────────
    // isViewed
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("isViewed — 단일 postId: 포함된 경우 → true")
    void isViewed_단일_포함() {
        String cookie = service.addViewed(null, 42L);
        assertThat(service.isViewed(cookie, 42L)).isTrue();
    }

    @Test
    @DisplayName("isViewed — 단일 postId: 없는 경우 → false")
    void isViewed_단일_미포함() {
        String cookie = service.addViewed(null, 42L);
        assertThat(service.isViewed(cookie, 99L)).isFalse();
    }

    @Test
    @DisplayName("isViewed — 여러 postId 중 포함 여부")
    void isViewed_복수() {
        String cookie = service.addViewed(null, 1L);
        cookie = service.addViewed(cookie, 2L);
        cookie = service.addViewed(cookie, 3L);

        assertThat(service.isViewed(cookie, 1L)).isTrue();
        assertThat(service.isViewed(cookie, 2L)).isTrue();
        assertThat(service.isViewed(cookie, 3L)).isTrue();
        assertThat(service.isViewed(cookie, 4L)).isFalse();
    }

    @Test
    @DisplayName("isViewed — 부분 일치는 false (ex. '4' 검색 시 '42' 에 매칭되지 않아야 함)")
    void isViewed_부분일치_방지() {
        String cookie = service.addViewed(null, 42L);
        assertThat(service.isViewed(cookie, 4L)).isFalse();
        assertThat(service.isViewed(cookie, 2L)).isFalse();
        assertThat(service.isViewed(cookie, 420L)).isFalse();
    }

    // ──────────────────────────────────────────────
    // secondsUntilMidnight
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("secondsUntilMidnight — 0 초과 86400 이하")
    void secondsUntilMidnight_범위() {
        int seconds = service.secondsUntilMidnight();
        assertThat(seconds).isGreaterThan(0).isLessThanOrEqualTo(86400);
    }

    // ──────────────────────────────────────────────
    // 통합 흐름
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("신규 조회 → addViewed → isValid → isViewed 전체 흐름")
    void 전체_흐름() {
        assertThat(service.isValid(null)).isFalse();

        String cookie = service.addViewed(null, 100L);
        assertThat(service.isValid(cookie)).isTrue();
        assertThat(service.isViewed(cookie, 100L)).isTrue();
        assertThat(service.isViewed(cookie, 200L)).isFalse();

        cookie = service.addViewed(cookie, 200L);
        assertThat(service.isValid(cookie)).isTrue();
        assertThat(service.isViewed(cookie, 100L)).isTrue();
        assertThat(service.isViewed(cookie, 200L)).isTrue();
        assertThat(service.isViewed(cookie, 300L)).isFalse();
    }

    private String todayKst() {
        return ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
