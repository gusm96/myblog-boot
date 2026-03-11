package com.moya.myblogboot;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 기반 통합 테스트 기반 클래스.
 * Singleton Container 패턴으로 Redis 컨테이너를 한 번만 시작하고 전체 테스트에서 공유한다.
 */
public abstract class AbstractContainerBaseTest {

    static final GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }
}
