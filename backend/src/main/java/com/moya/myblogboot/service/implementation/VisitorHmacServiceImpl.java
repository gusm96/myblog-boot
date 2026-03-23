package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.service.VisitorHmacService;
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
import java.util.UUID;

@Service
public class VisitorHmacServiceImpl implements VisitorHmacService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String DELIMITER = ":";

    private final String secretKey;

    public VisitorHmacServiceImpl(@Value("${visitor.hmac.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String generateToken() {
        String today = todayKst();
        String uuid = UUID.randomUUID().toString();
        String payload = today + DELIMITER + uuid;
        return payload + DELIMITER + sign(payload);
    }

    @Override
    public boolean isValid(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return false;
        int lastDelimiter = cookieValue.lastIndexOf(DELIMITER);
        if (lastDelimiter <= 0) return false;

        String payload = cookieValue.substring(0, lastDelimiter);
        String signature = cookieValue.substring(lastDelimiter + 1);

        String date = payload.split(DELIMITER, 2)[0];

        return todayKst().equals(date) && sign(payload).equals(signature);
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
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 실패", e);
        }
    }
}
