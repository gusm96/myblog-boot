package com.moya.myblogboot.configuration;

import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.implementation.AuthServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig  {
    @Value("${jwt.secret}")
    private String secret;
    private AuthService authService;
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.httpBasic().disable()
                .csrf().disable() // Cross Site Request Forgery (사이트간 위조요청) Non-browser clients service에선 disable 가능 (Spring security 에선 기본 설정이 protection)
                .cors().and() // Cross Origin Resource Sharing (교차 출처 리소스 공유)
                .authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/api/v1/boards").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/boards/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/boards/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/comment/**").hasAnyRole("NORMAL", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/comment/**").hasAnyRole("NORMAL", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/comment/**").hasAnyRole("NORMAL", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/likes/**").hasAnyRole("NORMAL", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/likes/**").hasAnyRole("NORMAL", "ADMIN")
                .requestMatchers(HttpMethod.DELETE,"/api/v1/likes/**").hasAnyRole("NORMAL", "ADMIN")
                .requestMatchers(new AntPathRequestMatcher("/api/v1/images/**")).hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // UsernamePasswordAuthenticationFilter 이전에 JwtFilter 적용
                .and().addFilterBefore(new JwtFilter(authService, secret), UsernamePasswordAuthenticationFilter.class).build();
    }

    // cors 허용을 위한 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Front
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 요청 HTTP Method
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 비밀번호 해싱을 위한 Bcrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
