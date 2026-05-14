package com.moya.myblogboot.constants;

import java.util.Arrays;
import java.util.List;

public class ShouldNotFilterPath {

    /**
     * 모든 HTTP 메서드에서 JWT 필터를 건너뛰는 경로.
     * 인증이 전혀 필요 없는 엔드포인트만 등록한다.
     */
    private static final List<String> EXCLUDE_ALL_METHODS = Arrays.asList(
            "/api/v1/login",            // POST  — 로그인
            "/api/v1/logout",           // GET   — 로그아웃
            "/api/v1/reissuing-token",  // GET   — RefreshToken → AccessToken 재발급
            "/api/v2/likes",            // GET/POST/DELETE — 쿠키 기반, JWT 불필요
            "/api/v2/visitor-count",    // GET   — 방문자 수 조회
            "/api/v2/categories",       // GET   — 공개 카테고리 목록 (V2)
            "/api/v8/posts",            // GET   — 레거시 게시글 조회
            "/api/v1/sse",              // GET   — SSE 스트림
            "/rss.xml"                  // GET   — RSS 피드
    );

    /**
     * GET 요청에서만 JWT 필터를 건너뛰는 경로.
     * GET은 공개이지만 POST/PUT/DELETE는 인증 정보가 필요한 엔드포인트.
     */
    private static final List<String> EXCLUDE_GET_ONLY = Arrays.asList(
            "/api/v1/posts",            // GET 목록/상세/slugs/category/search/views/likes — 공개
                                        // POST/PUT/DELETE — ADMIN 전용
            "/api/v1/categories",       // GET 목록 — 공개
                                        // POST/PUT/DELETE — ADMIN 전용
            "/api/v1/comments"          // GET 목록 — 공개
                                        // POST/PUT/DELETE — Principal로 어드민/비회원 구분
    );

    /**
     * 요청 경로와 HTTP 메서드를 기반으로 JWT 필터 제외 여부를 판단한다.
     * 경로 매칭은 정확히 일치하거나, excludePath + "/" 로 시작하는 하위 경로만 매칭한다.
     * (예: "/api/v1/categories" 는 "/api/v1/categories-management" 에 매칭되지 않음)
     */
    public static boolean shouldExclude(String path, String method) {
        if (EXCLUDE_ALL_METHODS.stream().anyMatch(p -> matchesPath(path, p))) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)
                && EXCLUDE_GET_ONLY.stream().anyMatch(p -> matchesPath(path, p))) {
            return true;
        }
        return false;
    }

    private static boolean matchesPath(String requestPath, String excludePath) {
        return requestPath.equals(excludePath)
                || requestPath.startsWith(excludePath + "/");
    }
}
