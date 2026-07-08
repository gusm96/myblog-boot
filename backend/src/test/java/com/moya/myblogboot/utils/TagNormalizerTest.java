package com.moya.myblogboot.utils;

import com.moya.myblogboot.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagNormalizerTest {

    @Test
    @DisplayName("태그 이름은 표시 원문을 trim하고 slug는 소문자 하이픈 형식으로 만든다")
    void normalizeNameAndSlug() {
        assertThat(TagNormalizer.normalizeName("  Spring Boot  ")).isEqualTo("Spring Boot");
        assertThat(TagNormalizer.toSlug("Spring Boot")).isEqualTo("spring-boot");
    }

    @Test
    @DisplayName("한글 태그 slug를 허용한다")
    void koreanSlug() {
        assertThat(TagNormalizer.toSlug("프론트엔드")).isEqualTo("프론트엔드");
    }

    @Test
    @DisplayName("정규화 후 빈 slug가 되면 거절한다")
    void rejectEmptySlug() {
        assertThatThrownBy(() -> TagNormalizer.toSlug("++"))
                .isInstanceOf(BusinessException.class);
    }
}
