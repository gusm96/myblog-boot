package com.moya.myblogboot.configuration;

import com.moya.myblogboot.constants.ShouldNotFilterPath;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.JwtUtil;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private String secret;
    private AuthService authService;

    public JwtFilter(AuthService authService, String secret) {
        this.authService = authService;
        this.secret = secret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        if (token.isEmpty()) {
            SecurityErrorResponseWriter.write(response, ErrorCode.INVALID_TOKEN);
            return;
        }

        try {
            JwtUtil.validateAccessToken(token, secret);
        } catch (SecurityException e) {
            SecurityErrorResponseWriter.write(response, ErrorCode.INVALID_TOKEN);
            return;
        } catch (ExpiredTokenException e) {
            SecurityErrorResponseWriter.write(response, ErrorCode.EXPIRED_TOKEN);
            return;
        } catch (InvalidateTokenException e) {
            SecurityErrorResponseWriter.write(response, e.getErrorCode());
            return;
        } catch (MalformedJwtException e) {
            SecurityErrorResponseWriter.write(response, ErrorCode.INVALID_TOKEN);
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
