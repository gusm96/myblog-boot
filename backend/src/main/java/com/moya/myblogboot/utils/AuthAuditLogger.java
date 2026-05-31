package com.moya.myblogboot.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final String UNKNOWN = "unknown";

    private final ClientIpResolver clientIpResolver;

    public AuthAuditLogger(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    public void success(AuthAuditEvent event, String username, HttpServletRequest request) {
        log(event, "SUCCESS", username, request, "none");
    }

    public void success(AuthAuditEvent event, HttpServletRequest request) {
        success(event, UNKNOWN, request);
    }

    public void failure(AuthAuditEvent event, String username, HttpServletRequest request, String reason) {
        log(event, "FAILURE", username, request, reason);
    }

    public void failure(AuthAuditEvent event, HttpServletRequest request, String reason) {
        failure(event, UNKNOWN, request, reason);
    }

    private void log(AuthAuditEvent event, String result, String username, HttpServletRequest request, String reason) {
        AUDIT.info("event={} result={} username={} ip={} ua={} reason={}",
                event,
                result,
                safe(username),
                safe(clientIpResolver.resolve(request)),
                safe(request.getHeader("User-Agent")),
                safe(reason));
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.replaceAll("\\s+", "_");
    }

    public enum AuthAuditEvent {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        TOKEN_REISSUE,
        TOKEN_REISSUE_REUSE_DETECTED,
        LOGIN_LOCKED
    }
}
