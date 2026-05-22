package com.moya.myblogboot.configuration;

import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.CookieFactory;
import com.moya.myblogboot.utils.JwtTokenProvider;
import com.moya.myblogboot.utils.TokenResolver;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.moya.myblogboot.constants.CookieName.ACCESS_TOKEN_COOKIE;
import static com.moya.myblogboot.support.TokenTestSupport.AUDIENCE;
import static com.moya.myblogboot.support.TokenTestSupport.ISSUER;
import static com.moya.myblogboot.support.TokenTestSupport.SECRET;
import static com.moya.myblogboot.support.TokenTestSupport.rawAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtFilterTest {

    private final CookieFactory cookieFactory =
            new CookieFactory(new CookieProperties(false, "Lax", "", "/"));
    private final TokenResolver tokenResolver = new TokenResolver();
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties());

    @Test
    @DisplayName("유효한 access_token 쿠키면 인증 정보를 설정한다")
    void validAccessTokenCookieAuthenticates() throws Exception {
        JwtFilter jwtFilter = newJwtFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        String token = jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");
        request.setCookies(new Cookie(ACCESS_TOKEN_COOKIE, token));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Authorization 헤더만 있으면 무시하고 미인증으로 통과한다")
    void authorizationHeaderOnlyIsIgnored() throws Exception {
        JwtFilter jwtFilter = newJwtFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("만료된 access_token 쿠키는 401 없이 만료 쿠키를 내려주고 미인증으로 통과한다")
    void expiredAccessTokenCookieExpiresCookieAndContinues() throws Exception {
        JwtFilter jwtFilter = newJwtFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        String expiredToken = rawAccessToken(-60_000L);
        request.setCookies(new Cookie(ACCESS_TOKEN_COOKIE, expiredToken));

        jwtFilter.doFilterInternal(request, response, filterChain);

        Cookie deletedCookie = response.getCookie(ACCESS_TOKEN_COOKIE);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(deletedCookie).isNotNull();
        assertThat(deletedCookie.getMaxAge()).isZero();
    }

    private JwtFilter newJwtFilter() {
        return new JwtFilter(mock(AuthService.class), jwtTokenProvider, tokenResolver, cookieFactory);
    }

    private JwtProperties jwtProperties() {
        return new JwtProperties(SECRET, ISSUER, AUDIENCE, 600_000L, 1_209_600_000L,
                2_592_000_000L, 10_000L);
    }
}
