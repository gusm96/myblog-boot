package com.moya.myblogboot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers + Docker Desktop (WSL2) 연결 검증용 스모크 테스트.
 * Spring 컨텍스트 없이 Redis 컨테이너 기동 여부만 확인한다.
 */
@Testcontainers
class TestcontainersConnectivityTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @Test
    @DisplayName("Redis 컨테이너 기동 및 포트 바인딩 검증")
    void redisContainerShouldStart() {
        assertThat(redis.isRunning()).isTrue();
        assertThat(redis.getMappedPort(6379)).isPositive();
        System.out.printf(
                "[Testcontainers] Redis 컨테이너 연결 성공: %s:%d%n",
                redis.getHost(),
                redis.getMappedPort(6379)
        );
    }
}
