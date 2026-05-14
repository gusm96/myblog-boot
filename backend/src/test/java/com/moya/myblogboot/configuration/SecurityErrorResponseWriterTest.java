package com.moya.myblogboot.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Security errors are written as ErrorResponse JSON")
    void write() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityErrorResponseWriter.write(response, ErrorCode.INVALID_TOKEN);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(ErrorCode.INVALID_TOKEN.getStatusValue());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(body.get("code").asText()).isEqualTo(ErrorCode.INVALID_TOKEN.getCode());
        assertThat(body.get("status").asInt()).isEqualTo(ErrorCode.INVALID_TOKEN.getStatusValue());
    }
}
