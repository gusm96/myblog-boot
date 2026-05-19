package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoginAttemptProperties.class)
public class LoginAttemptConfig {
}
