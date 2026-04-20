package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "revalidate.webhook")
public record RevalidateWebhookProperties(
        boolean enabled,
        String url,
        String secret,
        int connectTimeoutMs,
        int readTimeoutMs
) {}
