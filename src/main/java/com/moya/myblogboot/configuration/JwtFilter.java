package com.moya.myblogboot.configuration;

import com.moya.myblogboot.service.LoginService;
import com.moya.myblogboot.utils.JwtUtil;
import io.jsonwebtoken.Jwts;
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
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final LoginService loginService;
    private final String secretKey;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("Authorization : {}", authorization);

        // Token이 없을 시 Block
        if (authorization == null || !authorization.startsWith("Bearer ")) {
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
        if (JwtUtil.isExpired(token, secretKey)) {
            log.error("Token이 만료 되었습니다.");
            filterChain.doFilter(request,response);
            return;
        }

        String adminId = JwtUtil.getAdminId(token, secretKey);
        log.info("AdminId : {}", adminId);

        // 권한을 부여한다.
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(adminId, null, List.of(new SimpleGrantedAuthority("ADMIN")));
        // Detail을 넣어준다.
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request,response);
    }
}
