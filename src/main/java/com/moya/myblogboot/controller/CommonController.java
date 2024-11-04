package com.moya.myblogboot.controller;


import com.moya.myblogboot.dto.member.RandomUserNumberDto;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.service.RandomUserNumberService;
import com.moya.myblogboot.service.VisitorCountService;

import static com.moya.myblogboot.utils.DateUtil.*;

import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CommonController {
    private final RandomUserNumberService randomUserNumberService;
    private final VisitorCountService visitorCountService;

    // 유저번호 생성 및 방문자 수 조회
    @GetMapping("/api/v2/visitor-count")
    public ResponseEntity<VisitorCountDto> getVisitorCountV2(HttpServletResponse res, HttpServletRequest req) {
        Cookie userNumCookie = CookieUtil.findCookie(req, "user_n");
        String userNumValue = userNumCookie != null ? userNumCookie.getValue() : "";
        if (!isUserNumInvalid(userNumValue)) {
            return ResponseEntity.ok().body(createUserNumAndIncrementVisitorCount(res));
        } else {
            return handleExistingUserNum(res, userNumValue);
        }
    }

    private boolean isUserNumInvalid(String userNumValue) {
        return !userNumValue.isEmpty() && randomUserNumberService.isRandomUserNumberInRedis(Long.valueOf(userNumValue));
    }

    private ResponseEntity<VisitorCountDto> handleExistingUserNum(HttpServletResponse res, String userNumValue) {
        if (!randomUserNumberService.isRandomUserNumberInRedis(Long.valueOf(userNumValue))) {
            return ResponseEntity.ok().body(createUserNumAndIncrementVisitorCount(res));
        }
        return ResponseEntity.ok().body(visitorCountService.getVisitorCount(getToday()));
    }

    private VisitorCountDto createUserNumAndIncrementVisitorCount(HttpServletResponse res) {
        RandomUserNumberDto userNumberDto = randomUserNumberService.getRandomUserNumber();
        Cookie newUserNumCookie = new Cookie("user_n", String.valueOf(userNumberDto.getNumber()));
        newUserNumCookie.setMaxAge((int) userNumberDto.getExpireTime());
        newUserNumCookie.setHttpOnly(true);
        newUserNumCookie.setPath("/");
        res.addCookie(newUserNumCookie);
        return visitorCountService.incrementVisitorCount(getToday());
    }
}
