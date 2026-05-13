package com.moya.myblogboot.exception;

import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.ResponseEntity;

import static com.moya.myblogboot.constants.CookieName.REFRESH_TOKEN_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Refresh Token 만료 시 refresh_token 쿠키를 삭제한다")
    void handleExpiredRefreshTokenExceptionDeletesRefreshTokenCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie(REFRESH_TOKEN_COOKIE, "expired-token"));

        ResponseEntity<ErrorResponse> result = exceptionHandler.handleExpiredRefreshTokenException(
                request,
                response,
                new ExpiredRefreshTokenException()
        );

        Cookie deletedCookie = response.getCookie(REFRESH_TOKEN_COOKIE);
        assertThat(result.getStatusCode()).isEqualTo(ErrorCode.EXPIRED_REFRESH_TOKEN.getStatus());
        assertThat(deletedCookie).isNotNull();
        assertThat(deletedCookie.getMaxAge()).isZero();
    }
}
