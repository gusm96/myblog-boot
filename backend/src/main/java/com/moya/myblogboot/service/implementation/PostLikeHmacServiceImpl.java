package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.service.PostLikeHmacService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class PostLikeHmacServiceImpl extends HmacCookieSupport implements PostLikeHmacService {

    private static final String ID_DELIMITER = "_";

    public PostLikeHmacServiceImpl(@Value("${post.like.hmac.secret}") String secretKey) {
        super(secretKey);
    }

    @Override
    public boolean isValid(String cookieValue) {
        return verifiedPayload(cookieValue).isPresent();
    }

    @Override
    public boolean isLiked(String cookieValue, Long postId) {
        return verifiedPayload(cookieValue)
                .map(payload -> containsId(payload, postId))
                .orElse(false);
    }

    @Override
    public String addLike(String cookieValue, Long postId) {
        String newPayload;
        if (cookieValue == null || cookieValue.isBlank()) {
            newPayload = postId.toString();
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
    public String removeLike(String cookieValue, Long postId) {
        return verifiedPayload(cookieValue).map(payload -> {
            String newPayload = Arrays.stream(payload.split(ID_DELIMITER, -1))
                    .filter(id -> !id.equals(postId.toString()))
                    .collect(Collectors.joining(ID_DELIMITER));
            return newPayload.isBlank() ? null : newPayload + SIG_DELIMITER + sign(newPayload);
        }).orElse(null);
    }

    private boolean containsId(String payload, Long postId) {
        String target = postId.toString();
        for (String id : payload.split(ID_DELIMITER, -1)) {
            if (id.equals(target)) return true;
        }
        return false;
    }
}
