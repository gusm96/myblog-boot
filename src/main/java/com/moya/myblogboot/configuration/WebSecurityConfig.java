package com.moya.myblogboot.configuration;

import com.moya.myblogboot.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig{

    private final LoginService loginService;

    @Value("${jwt.secret}")
    private String secretKey;

    @Bean
    public SecurityFilterChain filterChain (HttpSecurity http) throws Exception {
        return http.httpBasic().disable()
                .csrf().disable()
                .cors().and()
                .authorizeHttpRequests()
                .requestMatchers(new AntPathRequestMatcher("/**")).permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/management/**").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/management/**").authenticated()
                .requestMatchers(HttpMethod.PUT,"/api/v1/management/**").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/management/**").authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().addFilterBefore(new JwtFilter(loginService, secretKey), UsernamePasswordAuthenticationFilter.class).build();
    }

}
