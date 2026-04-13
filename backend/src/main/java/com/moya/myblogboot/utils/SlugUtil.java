package com.moya.myblogboot.utils;

public class SlugUtil {

    private static final int MAX_LENGTH = 100;

    private SlugUtil() {}

    /**
     * 제목을 URL-safe slug로 변환한다.
     * - 한글은 그대로 유지 (Naver 한글 URL 지원)
     * - 영문은 소문자 변환
     * - 공백 → 하이픈
     * - 한글·영문·숫자·하이픈 외 문자 제거
     * - 연속 하이픈 → 단일 하이픈
     * - 앞뒤 하이픈 제거
     * - 100자 제한 (단어 경계 기준)
     */
    public static String generate(String title) {
        if (title == null || title.isBlank()) {
            return "post";
        }

        String slug = title.trim()
                .toLowerCase()
                // 허용 문자 외 제거 (한글 \uAC00-\uD7A3, 영문, 숫자, 공백, 하이픈)
                .replaceAll("[^\\uAC00-\\uD7A3a-z0-9\\s\\-]", "")
                // 공백 → 하이픈
                .replaceAll("\\s+", "-")
                // 연속 하이픈 → 단일 하이픈
                .replaceAll("-{2,}", "-")
                // 앞뒤 하이픈 제거
                .replaceAll("^-+|-+$", "");

        if (slug.isEmpty()) {
            return "post";
        }

        return truncate(slug);
    }

    /**
     * 중복 slug에 suffix를 붙인다: slug → slug-2 → slug-3
     */
    public static String withSuffix(String baseSlug, int suffix) {
        String suffixStr = "-" + suffix;
        int allowedBase = MAX_LENGTH - suffixStr.length();
        String base = baseSlug.length() > allowedBase
                ? baseSlug.substring(0, allowedBase)
                : baseSlug;
        return base + suffixStr;
    }

    private static String truncate(String slug) {
        if (slug.length() <= MAX_LENGTH) {
            return slug;
        }
        // 단어 경계(하이픈) 기준으로 자름
        String truncated = slug.substring(0, MAX_LENGTH);
        int lastHyphen = truncated.lastIndexOf('-');
        if (lastHyphen > MAX_LENGTH / 2) {
            return truncated.substring(0, lastHyphen);
        }
        return truncated;
    }
}
