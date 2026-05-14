package com.moya.myblogboot.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtFilterTest {

    private static final String SECRET = "12345678901234567890123456789012";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Invalid bearer token returns ErrorResponse JSON")
    void invalidBearerTokenReturnsErrorResponseJson() throws Exception {
        JwtFilter jwtFilter = new JwtFilter(mock(AuthService.class), SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        jwtFilter.doFilterInternal(request, response, filterChain);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(ErrorCode.INVALID_TOKEN.getStatusValue());
        assertThat(response.getContentType()).contains("application/json");
        assertThat(body.get("code").asText()).isEqualTo(ErrorCode.INVALID_TOKEN.getCode());
        assertThat(body.get("status").asInt()).isEqualTo(ErrorCode.INVALID_TOKEN.getStatusValue());
    }
}
