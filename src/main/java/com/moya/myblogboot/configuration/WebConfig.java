package com.moya.myblogboot.configuration;

import com.moya.myblogboot.interceptor.UserNumCookieInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final UserNumCookieInterceptor userNumCookieInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userNumCookieInterceptor)
                .addPathPatterns("/api/v2/visitor-count")
                .addPathPatterns("/api/v7/boards/**");
    }
}
