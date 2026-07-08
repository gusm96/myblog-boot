package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("blog.tag")
public record TagProperties(int minPerPost, int maxPerPost) {
    public TagProperties {
        if (minPerPost == 0) {
            minPerPost = 1;
        }
        if (maxPerPost == 0) {
            maxPerPost = 5;
        }
    }
}
