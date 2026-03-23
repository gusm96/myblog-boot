package com.moya.myblogboot.service;

public interface VisitorHmacService {
    /** 오늘(KST) 날짜 기반 HMAC 서명 토큰 생성 */
    String generateToken();

    /** 쿠키 값이 오늘(KST) 날짜 기준 유효한 토큰인지 검증 */
    boolean isValid(String cookieValue);

    /** 쿠키 Max-Age: 현재 시각 기준 KST 자정까지 남은 초 */
    int secondsUntilMidnight();
}
