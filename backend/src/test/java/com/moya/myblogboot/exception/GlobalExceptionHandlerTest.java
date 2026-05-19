package com.moya.myblogboot.exception;

import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.configuration.CookieProperties;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.utils.CookieFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.ResponseEntity;

import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;
import static com.moya.myblogboot.constants.CookieName.REFRESH_TOKEN_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(
            new CookieFactory(new CookieProperties(false, "Lax", "", "/"))
    );

    @Test
    @DisplayName("Refresh Token 만료 시 auth 쿠키 전체를 삭제한다")
    void handleExpiredRefreshTokenExceptionDeletesRefreshTokenCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ErrorResponse> result = exceptionHandler.handleExpiredRefreshTokenException(
                response,
                new ExpiredRefreshTokenException()
        );

        Cookie deletedCookie = response.getCookie(REFRESH_TOKEN_COOKIE);
        Cookie deletedAccessCookie = response.getCookie(ACCESS_TOKEN_COOKIE);
        assertThat(result.getStatusCode()).isEqualTo(ErrorCode.EXPIRED_REFRESH_TOKEN.getStatus());
        assertThat(deletedCookie).isNotNull();
        assertThat(deletedCookie.getMaxAge()).isZero();
        assertThat(deletedAccessCookie).isNotNull();
        assertThat(deletedAccessCookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 예외 시 auth 쿠키 전체를 삭제한다")
    void handleInvalidateTokenExceptionDeletesAuthCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ErrorResponse> result = exceptionHandler.handleInvalidateTokenException(
                response,
                new InvalidateTokenException()
        );

        assertThat(result.getStatusCode()).isEqualTo(ErrorCode.INVALID_TOKEN.getStatus());
        assertThat(response.getCookie(REFRESH_TOKEN_COOKIE).getMaxAge()).isZero();
        assertThat(response.getCookie(ACCESS_TOKEN_COOKIE).getMaxAge()).isZero();
    }
}
