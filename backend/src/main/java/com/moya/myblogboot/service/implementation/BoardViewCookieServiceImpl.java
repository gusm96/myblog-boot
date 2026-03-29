package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.service.BoardViewCookieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class BoardViewCookieServiceImpl implements BoardViewCookieService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 쿠키 형식: {date}:{boardId1}_{boardId2}_...|{HMAC_SIGNATURE}
    // ','는 RFC 6265 금지 문자(0x2C)라 Tomcat이 IllegalArgumentException을 throw → '_' 사용
    private static final char SIG_DELIMITER = '|';
    private static final char DATE_DELIMITER = ':';
    private static final String ID_DELIMITER = "_";

    private final String secretKey;

    public BoardViewCookieServiceImpl(@Value("${board.view.hmac.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public boolean isValid(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return false;
        int lastPipe = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (lastPipe <= 0) return false;

        String payload = cookieValue.substring(0, lastPipe);
        String sig = cookieValue.substring(lastPipe + 1);

        int colonIdx = payload.indexOf(DATE_DELIMITER);
        if (colonIdx <= 0) return false;
        String date = payload.substring(0, colonIdx);

        return todayKst().equals(date) && sign(payload).equals(sig);
    }

    @Override
    public boolean isViewed(String cookieValue, Long boardId) {
        int lastPipe = cookieValue.lastIndexOf(SIG_DELIMITER);
        String payload = cookieValue.substring(0, lastPipe);

        int colonIdx = payload.indexOf(DATE_DELIMITER);
        String idsPart = payload.substring(colonIdx + 1);

        String target = boardId.toString();
        for (String id : idsPart.split(ID_DELIMITER, -1)) {
            if (id.equals(target)) return true;
        }
        return false;
    }

    @Override
    public String addViewed(String cookieValue, Long boardId) {
        String newPayload;
        if (cookieValue == null) {
            newPayload = todayKst() + DATE_DELIMITER + boardId;
        } else {
            int lastPipe = cookieValue.lastIndexOf(SIG_DELIMITER);
            String existingPayload = cookieValue.substring(0, lastPipe);
            newPayload = existingPayload + ID_DELIMITER + boardId;
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

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 실패", e);
        }
    }
}
