package com.moya.myblogboot.interceptor;

import com.moya.myblogboot.service.VisitorHmacService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.moya.myblogboot.constants.CookieName.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNumCookieInterceptor implements HandlerInterceptor {
    public static final String IS_NEW_VISITOR = "IS_NEW_VISITOR";
    private final VisitorHmacService visitorHmacService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Cookie userNumCookie = CookieUtil.findCookie(request, USER_NUM_COOKIE);
        String cookieValue = userNumCookie != null ? userNumCookie.getValue() : null;

        boolean isNewVisitor = false;
        if (!visitorHmacService.isValid(cookieValue)) {
            String newToken = visitorHmacService.generateToken();
            int maxAge = visitorHmacService.secondsUntilMidnight();
            response.addCookie(CookieUtil.addCookie(USER_NUM_COOKIE, newToken, maxAge));
            cookieValue = newToken;
            isNewVisitor = true;
        }

        request.setAttribute(USER_NUM_COOKIE, cookieValue);
        request.setAttribute(IS_NEW_VISITOR, isNewVisitor);

        return true;
    }
}
