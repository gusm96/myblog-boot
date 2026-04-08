package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.service.BoardLikeHmacService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
public class BoardLikeHmacServiceImpl implements BoardLikeHmacService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char SIG_DELIMITER = '|';
    private static final String ID_DELIMITER = "_";

    private final String secretKey;

    public BoardLikeHmacServiceImpl(
            @Value("${board.like.hmac.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public boolean isValid(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return false;
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return false;

        String payload = cookieValue.substring(0, pipeIdx);
        String sig = cookieValue.substring(pipeIdx + 1);
        return sign(payload).equals(sig);
    }

    @Override
    public boolean isLiked(String cookieValue, Long boardId) {
        if (cookieValue == null) return false;
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return false;

        String payload = cookieValue.substring(0, pipeIdx);
        String target = boardId.toString();
        for (String id : payload.split(ID_DELIMITER, -1)) {
            if (id.equals(target)) return true;
        }
        return false;
    }

    @Override
    public String addLike(String cookieValue, Long boardId) {
        String newPayload;
        if (cookieValue == null || cookieValue.isBlank()) {
            newPayload = boardId.toString();
        } else {
            int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
            String existingPayload = (pipeIdx > 0)
                    ? cookieValue.substring(0, pipeIdx)
                    : cookieValue;
            newPayload = existingPayload + ID_DELIMITER + boardId;
        }
        return newPayload + SIG_DELIMITER + sign(newPayload);
    }

    @Override
    public String removeLike(String cookieValue, Long boardId) {
        if (cookieValue == null) return null;
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return null;

        String payload = cookieValue.substring(0, pipeIdx);
        String target = boardId.toString();

        String newPayload = Arrays.stream(payload.split(ID_DELIMITER, -1))
                .filter(id -> !id.equals(target))
                .collect(Collectors.joining(ID_DELIMITER));

        if (newPayload.isBlank()) return null;
        return newPayload + SIG_DELIMITER + sign(newPayload);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 실패", e);
        }
    }
}
