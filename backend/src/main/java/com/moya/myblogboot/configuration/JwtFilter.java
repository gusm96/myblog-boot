package com.moya.myblogboot.configuration;

import com.moya.myblogboot.constants.ShouldNotFilterPath;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.CookieFactory;
import com.moya.myblogboot.utils.JwtUtil;
import com.moya.myblogboot.utils.TokenResolver;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final String secret;
    private final AuthService authService;
    private final TokenResolver tokenResolver;
    private final CookieFactory cookieFactory;

    public JwtFilter(AuthService authService, String secret,
                     TokenResolver tokenResolver, CookieFactory cookieFactory) {
        this.authService = authService;
        this.secret = secret;
        this.tokenResolver = tokenResolver;
        this.cookieFactory = cookieFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = tokenResolver.resolve(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtUtil.validateAccessToken(token, secret);
        } catch (ExpiredTokenException | InvalidateTokenException | JwtException | IllegalArgumentException e) {
            response.addCookie(cookieFactory.expireAccessTokenCookie());
            filterChain.doFilter(request, response);
            return;
        }

        AccessTokenClaims tokenInfo = JwtUtil.parseAccessToken(token, secret);
        Long memberPrimaryKey = tokenInfo.memberPrimaryKey();

        log.debug("{} {} | member={} role={}", request.getMethod(),
                request.getRequestURI(), memberPrimaryKey, tokenInfo.role());

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(memberPrimaryKey, null, List.of(new SimpleGrantedAuthority(tokenInfo.role())));
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return ShouldNotFilterPath.shouldExclude(request.getRequestURI(), request.getMethod());
    }
}
