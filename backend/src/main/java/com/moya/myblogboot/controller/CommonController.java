package com.moya.myblogboot.controller;


import com.moya.myblogboot.dto.visitor.VisitorCountDto;
import com.moya.myblogboot.service.VisitorCountService;
import com.moya.myblogboot.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CommonController {
    private final VisitorCountService visitorCountService;

    // 유저번호 생성 및 방문자 수 조회
    @GetMapping("/api/v2/visitor-count")
    public ResponseEntity<VisitorCountDto> getVisitorCountV2() {
        VisitorCountDto visitorCountDto = visitorCountService.getVisitorCount(DateUtil.getToday());
        return ResponseEntity.ok().body(visitorCountDto);
    }
}
