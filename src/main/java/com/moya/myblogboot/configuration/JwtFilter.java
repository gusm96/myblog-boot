package com.moya.myblogboot.configuration;

import com.moya.myblogboot.service.LoginService;
import com.moya.myblogboot.utils.JwtUtil;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private  String secretKey;
    private  LoginService loginService;

    public JwtFilter(LoginService loginService, String secretKey) {
        this.loginService = loginService;
        this.secretKey = secretKey;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("Authorization : {}", authorization);

        // Token이 없을 시 Block
        if (authorization == null || !authorization.startsWith("bearer ")) {
            log.error("Authorization is null.");
            filterChain.doFilter(request,response);
            return;
        }
        // Token 꺼내기
        String token = authorization.split(" ")[1];
        if(token.isEmpty()){
            log.error("Token is null");
        }

        // Token Expired되었는지 여부
        try {
            if (JwtUtil.isExpired(token, secretKey)) {
                log.error("Token is expired");
                filterChain.doFilter(request, response);
                return;
            }
        } catch (SignatureException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not a valid token");
            return;
        }

        String adminName = JwtUtil.getAdminName(token, secretKey);
        log.info("admin_name : {}", adminName);

        // 권한을 부여한다.
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(adminName, null, List.of(new SimpleGrantedAuthority("ADMIN")));
        // Detail을 넣어준다.
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 필터에서 제외시킬 url
        String[] excludePath = {
                "/api/v1/login/admin",
                "/api/v1/boards",
                "/api/v1/boards/search",
                "/api/v1/board",
                "/api/v1/categories",
                "/api/v1/comment",
                "/api/v1/comments",
                "/api/v1/guest",
                "/api/v1/login/guest"
        };

        String path = request.getRequestURI();
        log.info(path);
        return (Arrays.stream(excludePath).anyMatch(path::startsWith));
    }
}