package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.service.PostViewCookieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PostViewCookieServiceImpl extends HmacCookieSupport implements PostViewCookieService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 쿠키 형식: {date}:{postId1}_{postId2}_...|{HMAC_SIGNATURE}
    // ','는 RFC 6265 금지 문자(0x2C)라 Tomcat이 IllegalArgumentException을 throw → '_' 사용
    private static final char DATE_DELIMITER = ':';
    private static final String ID_DELIMITER = "_";

    public PostViewCookieServiceImpl(@Value("${post.view.hmac.secret}") String secretKey) {
        super(secretKey);
    }

    @Override
    public boolean isValid(String cookieValue) {
        return verifiedPayload(cookieValue)
                .map(payload -> {
                    int colonIdx = payload.indexOf(DATE_DELIMITER);
                    return colonIdx > 0 && todayKst().equals(payload.substring(0, colonIdx));
                })
                .orElse(false);
    }

    @Override
    public boolean isViewed(String cookieValue, Long postId) {
        return verifiedPayload(cookieValue)
                .map(payload -> {
                    int colonIdx = payload.indexOf(DATE_DELIMITER);
                    String idsPart = payload.substring(colonIdx + 1);
                    String target = postId.toString();
                    for (String id : idsPart.split(ID_DELIMITER, -1)) {
                        if (id.equals(target)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    public String addViewed(String cookieValue, Long postId) {
        String newPayload;
        if (cookieValue == null) {
            newPayload = todayKst() + DATE_DELIMITER + postId;
        } else {
            String existingPayload = verifiedPayload(cookieValue).orElseGet(() -> {
                int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
                return pipeIdx > 0 ? cookieValue.substring(0, pipeIdx) : cookieValue;
            });
            newPayload = existingPayload + ID_DELIMITER + postId;
        }
        return newPayload + SIG_DELIMITER + sign(newPayload);
    }

    @Override
    public int secondsUntilMidnight() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
        return (int) Duration.between(now, midnight).getSeconds();
    }

    private String todayKst() {
        return ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
