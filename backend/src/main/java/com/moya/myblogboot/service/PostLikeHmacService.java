package com.moya.myblogboot.service;

public interface PostLikeHmacService {

    /** 쿠키 서명 검증 */
    boolean isValid(String cookieValue);

    /** postId가 좋아요 목록에 있는지 확인 */
    boolean isLiked(String cookieValue, Long postId);

    /** 좋아요 추가 → 새 쿠키 값 반환 */
    String addLike(String cookieValue, Long postId);

    /** 좋아요 취소 → 새 쿠키 값 반환 (남은 항목 없으면 null) */
    String removeLike(String cookieValue, Long postId);
}
