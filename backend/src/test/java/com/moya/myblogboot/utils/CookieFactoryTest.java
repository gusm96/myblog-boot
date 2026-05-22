package com.moya.myblogboot.utils;

import com.moya.myblogboot.configuration.CookieProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;
import static com.moya.myblogboot.constants.CookieName.REFRESH_TOKEN_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

class CookieFactoryTest {

    @Test
    @DisplayName("개발 프로파일 쿠키 속성을 적용한다")
    void createsDevCookiePolicy() {
        CookieFactory cookieFactory = new CookieFactory(new CookieProperties(false, "Lax", "", "/"));

        Cookie cookie = cookieFactory.accessTokenCookie("access-token", 600);

        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getMaxAge()).isEqualTo(600);
    }

    @Test
    @DisplayName("domain 설정이 있으면 생성/삭제 쿠키에 동일하게 반영한다")
    void appliesDomainToCreateAndExpireCookies() {
        CookieFactory cookieFactory = new CookieFactory(new CookieProperties(true, "Lax", "example.com", "/"));

        Cookie cookie = cookieFactory.refreshTokenCookie("refresh-token", 1200);
        Cookie expired = cookieFactory.expire(REFRESH_TOKEN_COOKIE);

        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getDomain()).isEqualTo("example.com");
        assertThat(expired.getDomain()).isEqualTo("example.com");
        assertThat(expired.getPath()).isEqualTo(cookie.getPath());
        assertThat(expired.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("auth 쿠키 전체 삭제는 refresh_token과 access_token을 함께 만료한다")
    void expiresAllAuthCookies() {
        CookieFactory cookieFactory = new CookieFactory(new CookieProperties(false, "Lax", "", "/"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieFactory.expireAuthCookies(response);

        assertThat(response.getCookie(REFRESH_TOKEN_COOKIE).getMaxAge()).isZero();
        assertThat(response.getCookie(ACCESS_TOKEN_COOKIE).getMaxAge()).isZero();
    }
}
