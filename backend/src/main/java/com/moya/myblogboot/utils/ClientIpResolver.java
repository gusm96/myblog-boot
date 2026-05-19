package com.moya.myblogboot.utils;

import com.moya.myblogboot.configuration.LoginAttemptProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN = "unknown";

    private final LoginAttemptProperties properties;

    public String resolve(HttpServletRequest request) {
        if (properties.trustedProxy()) {
            String forwardedFor = request.getHeader(X_FORWARDED_FOR);
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String firstIp = forwardedFor.split(",")[0].trim();
                if (!firstIp.isBlank() && !UNKNOWN.equalsIgnoreCase(firstIp)) {
                    return firstIp;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? UNKNOWN : remoteAddr;
    }
}
