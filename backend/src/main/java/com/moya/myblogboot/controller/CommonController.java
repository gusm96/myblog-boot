package com.moya.myblogboot.controller;


import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.service.VisitorCountService;
import com.moya.myblogboot.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import static com.moya.myblogboot.interceptor.UserNumCookieInterceptor.IS_NEW_VISITOR;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CommonController {
    private final VisitorCountService visitorCountService;

    @GetMapping("/api/v2/visitor-count")
    public ResponseEntity<VisitorCountDto> getVisitorCountV2(
            @RequestAttribute(name = IS_NEW_VISITOR, required = false) Boolean isNewVisitor) {
        String today = DateUtil.getToday();
        if (Boolean.TRUE.equals(isNewVisitor)) {
            return ResponseEntity.ok().body(visitorCountService.incrementVisitorCount(today));
        }
        return ResponseEntity.ok().body(visitorCountService.getVisitorCount(today));
    }
}
