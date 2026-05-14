package com.moya.myblogboot.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponseWriter() {
    }

    public static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatusValue());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
