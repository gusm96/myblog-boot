package com.moya.myblogboot;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

public abstract class AbstractContainerBaseTest {
    static final String REDIS_IMAGE = "redis:6.0.16";
    static final GenericContainer REDIS_CONTAINER;

    static {
        REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379)
                .withReuse(true);
        REDIS_CONTAINER.start();
    }

    @AfterAll
    public static void tearDown() {
        REDIS_CONTAINER.stop();
    }
    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry){
        registry.add("spring.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.redis.port", () -> ""+REDIS_CONTAINER.getMappedPort(6379));
    }

}
