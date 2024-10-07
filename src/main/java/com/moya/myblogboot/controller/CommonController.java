package com.moya.myblogboot.controller;


import com.moya.myblogboot.dto.member.RandomUserNumberDto;
import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.service.RandomUserNumberService;
import com.moya.myblogboot.service.VisitorCountService;
import static com.moya.myblogboot.utils.DateUtil.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CommonController {
    private final RandomUserNumberService randomUserNumberService;
    private final VisitorCountService visitorCountService;

    // 랜덤 유저번호 생성
    @GetMapping("/api/v1/generate-user-number")
    public ResponseEntity<?> generateUserNumber(HttpServletResponse res) {
        // 임시 번호 생성
        try {
            RandomUserNumberDto randomUserNumber = randomUserNumberService.getRandomUserNumber();

            Cookie userNumCookie = new Cookie("user_n", String.valueOf(randomUserNumber.getNumber()));
            userNumCookie.setMaxAge((int) randomUserNumber.getExpireTime());
            userNumCookie.setPath("/");
            res.addCookie(userNumCookie);
            // Visitor Count 방문자 수 증가
            VisitorCountDto visitorCountDto = visitorCountService.incrementVisitorCount(getToday());
            return ResponseEntity.ok().body(visitorCountDto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 방문자 수
    @GetMapping("/api/v1/visitor-count")
    public ResponseEntity<VisitorCountDto> getVisitorCount() {
        VisitorCountDto visitorCountDto = visitorCountService.getVisitorCount(getToday());
        return ResponseEntity.ok().body(visitorCountDto);
    }
}
