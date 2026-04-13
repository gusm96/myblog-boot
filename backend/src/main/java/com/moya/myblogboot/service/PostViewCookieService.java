package com.moya.myblogboot.service;

public interface PostViewCookieService {

    /** 쿠키 값이 유효한지 검증 (HMAC 서명 + 오늘 KST 날짜 일치 여부) */
    boolean isValid(String cookieValue);

    /** 오늘 이미 조회한 게시글인지 확인. isValid() == true 인 경우에만 호출할 것 */
    boolean isViewed(String cookieValue, Long postId);

    /**
     * postId를 목록에 추가하고 재서명한 새 쿠키 값을 반환.
     * cookieValue == null 이면 오늘 날짜 기준으로 새로 생성.
     */
    String addViewed(String cookieValue, Long postId);

    /** KST 자정까지 남은 초 (쿠키 Max-Age 용) */
    int secondsUntilMidnight();
}
