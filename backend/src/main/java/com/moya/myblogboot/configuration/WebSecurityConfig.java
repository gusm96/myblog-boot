package com.moya.myblogboot.configuration;

import com.moya.myblogboot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    private final AuthService authService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/boards").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/boards/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/boards/**").hasRole("ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/deleted-boards/**")).hasRole("ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/management/**")).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/comments/**").hasAnyRole("NORMAL", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/comments/**").hasAnyRole("NORMAL", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/comments/**").hasAnyRole("NORMAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories-management/**").hasRole("ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/images/**")).hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(new JwtFilter(authService, secret), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // cors 허용을 위한 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE")); // 요청 HTTP Method
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
