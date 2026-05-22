package com.moya.myblogboot.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;

@Component
public class TokenResolver {

    public String resolve(HttpServletRequest request) {
        Cookie cookie = CookieUtil.findCookie(request, ACCESS_TOKEN_COOKIE);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }
        return cookie.getValue();
    }
}
