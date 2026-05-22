package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cookie")
public record CookieProperties(
        boolean secure,
        String sameSite,
        String domain,
        String path
) {
}
