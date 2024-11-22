package com.moya.myblogboot.interceptor;

import com.moya.myblogboot.dto.member.RandomUserNumberDto;
import com.moya.myblogboot.service.RandomUserNumberService;
import com.moya.myblogboot.service.VisitorCountService;
import com.moya.myblogboot.utils.CookieUtil;
import com.moya.myblogboot.utils.DateUtil;
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
    private final RandomUserNumberService randomUserNumberService;
    private final VisitorCountService visitorCountService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Finding user_n cookies
        Cookie userNumCookie = CookieUtil.findCookie(request, USER_N);
        String userNumValue = userNumCookie != null ? userNumCookie.getValue() : "";

        // user_n cookie validation
        if (!validateUserNum(userNumValue)) {
            // invalid user num
            RandomUserNumberDto userNumberDto = randomUserNumberService.getRandomUserNumber();
            Cookie newUserNumCookie = createUserNumCookie(userNumberDto);
            response.addCookie(newUserNumCookie);
            userNumValue = String.valueOf(userNumberDto.getNumber());
            // Store in Redis and increment the visitor count.
            visitorCountService.incrementVisitorCount(DateUtil.getToday());
        }
        request.setAttribute(USER_N, userNumValue);

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    private static Cookie createUserNumCookie(RandomUserNumberDto userNumberDto) {
        Cookie newUserNumCookie = new Cookie(USER_N, String.valueOf(userNumberDto.getNumber()));
        newUserNumCookie.setMaxAge((int) userNumberDto.getExpireTime());
        newUserNumCookie.setHttpOnly(true);
        newUserNumCookie.setPath("/");
        return newUserNumCookie;
    }

    private boolean validateUserNum(String userNumValue) {
        return !userNumValue.isEmpty() && randomUserNumberService.isRandomUserNumberInRedis(Long.valueOf(userNumValue));
    }

}
