package com.moya.myblogboot.utils;

import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;

public final class TagNormalizer {

    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_SLUG_LENGTH = 60;
    private static final String SLUG_PATTERN = "^[a-z0-9가-힣\\-]+$";

    private TagNormalizer() {
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TAG_NAME);
        }
        String normalized = name.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_TAG_NAME);
        }
        return normalized;
    }

    public static String toSlug(String name) {
        String slug = SlugUtil.generate(normalizeName(name));
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        if (slug.isBlank() || !slug.matches(SLUG_PATTERN) || "post".equals(slug)) {
            throw new BusinessException(ErrorCode.INVALID_TAG_SLUG);
        }
        return slug;
    }
}
