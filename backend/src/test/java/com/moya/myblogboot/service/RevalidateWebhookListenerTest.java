package com.moya.myblogboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.configuration.RevalidateWebhookConfig;
import com.moya.myblogboot.configuration.RevalidateWebhookProperties;
import com.moya.myblogboot.domain.event.CategoryChangeEvent;
import com.moya.myblogboot.domain.event.PostChangeEvent;
import com.moya.myblogboot.dto.revalidate.RevalidateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RevalidateWebhookListenerTest {

    private static final String WEBHOOK_URL = "http://localhost:3000/api/revalidate";
    private static final String SECRET = "test-secret";

    private MockRestServiceServer mockServer;
    private RevalidateWebhookListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RevalidateWebhookProperties props = new RevalidateWebhookProperties(
                true, WEBHOOK_URL, SECRET, 2000, 3000
        );
        RevalidateWebhookConfig config = new RevalidateWebhookConfig();
        RestTemplate restTemplate = config.revalidateRestTemplate(props);
        mockServer = MockRestServiceServer.createServer(restTemplate);

        RevalidateWebhookClient client = new RevalidateWebhookClient(props, restTemplate);
        listener = new RevalidateWebhookListener(client);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Post CREATED — tags: [posts, slugs], paths: [/sitemap.xml]")
    void postCreated() throws Exception {
        mockServer.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-revalidate-secret", SECRET))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(toJson(new RevalidateRequest(
                        List.of("posts", "slugs"), List.of("/sitemap.xml")
                ))))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        listener.onPostChange(new PostChangeEvent(this, "CREATED", 1L, "my-post"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Post UPDATED — tags: [posts, post:{slug}], paths: []")
    void postUpdated() throws Exception {
        mockServer.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-revalidate-secret", SECRET))
                .andExpect(content().json(toJson(new RevalidateRequest(
                        List.of("posts", "post:my-post"), List.of()
                ))))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        listener.onPostChange(new PostChangeEvent(this, "UPDATED", 1L, "my-post"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Post DELETED — tags: [posts, post:{slug}, slugs], paths: [/sitemap.xml]")
    void postDeleted() throws Exception {
        mockServer.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-revalidate-secret", SECRET))
                .andExpect(content().json(toJson(new RevalidateRequest(
                        List.of("posts", "post:my-post", "slugs"), List.of("/sitemap.xml")
                ))))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        listener.onPostChange(new PostChangeEvent(this, "DELETED", 1L, "my-post"));

        mockServer.verify();
    }

    @Test
    @DisplayName("Category CREATED — tags: [categories], paths: []")
    void categoryCreated() throws Exception {
        mockServer.expect(requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-revalidate-secret", SECRET))
                .andExpect(content().json(toJson(new RevalidateRequest(
                        List.of("categories"), List.of()
                ))))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        listener.onCategoryChange(new CategoryChangeEvent(this, "CREATED", 5L));

        mockServer.verify();
    }

    @Test
    @DisplayName("Webhook 5xx 응답 — 1회 재시도 후 warn, 예외 전파 없음")
    void webhookServerError() {
        mockServer.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());
        mockServer.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError());

        listener.onPostChange(new PostChangeEvent(this, "CREATED", 1L, "my-post"));

        mockServer.verify();
    }

    @Test
    @DisplayName("enabled=false — RestTemplate 호출 0회")
    void disabledSkipsCall() {
        RevalidateWebhookProperties disabledProps = new RevalidateWebhookProperties(
                false, WEBHOOK_URL, SECRET, 2000, 3000
        );
        RevalidateWebhookConfig config = new RevalidateWebhookConfig();
        RestTemplate rt = config.revalidateRestTemplate(disabledProps);
        MockRestServiceServer disabledServer = MockRestServiceServer.createServer(rt);

        RevalidateWebhookClient disabledClient = new RevalidateWebhookClient(disabledProps, rt);
        RevalidateWebhookListener disabledListener = new RevalidateWebhookListener(disabledClient);

        disabledListener.onPostChange(new PostChangeEvent(this, "CREATED", 1L, "my-post"));

        disabledServer.verify(); // expect 없이 verify → 호출 0회 확인
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
