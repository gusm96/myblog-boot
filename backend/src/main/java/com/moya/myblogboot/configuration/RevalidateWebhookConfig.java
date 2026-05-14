package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(RevalidateWebhookProperties.class)
public class RevalidateWebhookConfig {

    @Bean("revalidateRestTemplate")
    public RestTemplate revalidateRestTemplate(RevalidateWebhookProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.readTimeoutMs());
        return new RestTemplate(factory);
    }
}
