package com.moya.myblogboot.utils;

import com.moya.myblogboot.configuration.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;
import static com.moya.myblogboot.constants.CookieName.REFRESH_TOKEN_COOKIE;

@Component
@RequiredArgsConstructor
public class CookieFactory {

    private final CookieProperties properties;

    public Cookie accessTokenCookie(String value, int maxAge) {
        return authCookie(ACCESS_TOKEN_COOKIE, value, maxAge);
    }

    public Cookie refreshTokenCookie(String value, int maxAge) {
        return authCookie(REFRESH_TOKEN_COOKIE, value, maxAge);
    }

    public Cookie expireAccessTokenCookie() {
        return expire(ACCESS_TOKEN_COOKIE);
    }

    public Cookie expire(String name) {
        return authCookie(name, "", 0);
    }

    public void expireAuthCookies(HttpServletResponse response) {
        response.addCookie(expire(REFRESH_TOKEN_COOKIE));
        response.addCookie(expire(ACCESS_TOKEN_COOKIE));
    }

    private Cookie authCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path());
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.secure());
        cookie.setAttribute("SameSite", sameSite());
        cookie.setMaxAge(maxAge);
        if (StringUtils.hasText(properties.domain())) {
            cookie.setDomain(properties.domain());
        }
        return cookie;
    }

    private String path() {
        return StringUtils.hasText(properties.path()) ? properties.path() : "/";
    }

    private String sameSite() {
        return StringUtils.hasText(properties.sameSite()) ? properties.sameSite() : "Lax";
    }
}
