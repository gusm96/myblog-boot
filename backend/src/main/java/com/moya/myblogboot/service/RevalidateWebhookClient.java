package com.moya.myblogboot.service;

import com.moya.myblogboot.configuration.RevalidateWebhookProperties;
import com.moya.myblogboot.dto.revalidate.RevalidateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RevalidateWebhookClient {

    private final RevalidateWebhookProperties props;
    @Qualifier("revalidateRestTemplate")
    private final RestTemplate restTemplate;

    public void send(List<String> tags, List<String> paths) {
        if (!props.enabled()) return;
        if ((tags == null || tags.isEmpty()) && (paths == null || paths.isEmpty())) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-revalidate-secret", props.secret());
        HttpEntity<RevalidateRequest> entity = new HttpEntity<>(RevalidateRequest.of(tags, paths), headers);

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                restTemplate.postForEntity(props.url(), entity, String.class);
                log.info("[revalidate] ok tags={} paths={}", tags, paths);
                return;
            } catch (RestClientException e) {
                if (attempt == 2) {
                    log.warn("[revalidate] failed tags={} paths={} error={}", tags, paths, e.getMessage());
                }
            }
        }
    }
}
