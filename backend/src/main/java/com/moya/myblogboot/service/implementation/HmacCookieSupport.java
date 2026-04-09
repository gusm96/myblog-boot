package com.moya.myblogboot.service.implementation;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * HMAC-SHA256 서명 쿠키의 공통 로직을 제공하는 추상 클래스.
 * 쿠키 형식: {payload}|{HMAC_SIGNATURE}
 */
abstract class HmacCookieSupport {

    protected static final String ALGORITHM = "HmacSHA256";
    protected static final char SIG_DELIMITER = '|';

    protected final String secretKey;

    protected HmacCookieSupport(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 쿠키 값의 HMAC 서명을 검증하고, 유효하면 페이로드를 반환한다.
     * null·blank이거나 구분자가 없거나 서명이 불일치하면 Optional.empty()를 반환한다.
     */
    protected Optional<String> verifiedPayload(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return Optional.empty();
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return Optional.empty();

        String payload = cookieValue.substring(0, pipeIdx);
        String sig = cookieValue.substring(pipeIdx + 1);
        return sign(payload).equals(sig) ? Optional.of(payload) : Optional.empty();
    }

    protected String sign(String data) {
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
