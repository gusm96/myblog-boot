package com.moya.myblogboot.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Getter
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.timeout}")
    private int timeout;
    @Value("${spring.data.redis.database}")
    private int database;
    private static final String REDISSON_HOST_PREFIX = "redis://";

    // Redis 연결
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        final SocketOptions socketoptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(10)) // 연결 시간제한.
                .build();
        final ClientOptions clientoptions = ClientOptions.builder()
                .socketOptions(socketoptions)
                .build();
        LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
                .clientOptions(clientoptions)
                .commandTimeout(Duration.ofMinutes(1)) // 명령 최대시간.
                .shutdownTimeout(Duration.ZERO) // Redis 연결 종료시 타임아웃 설정. (강제 종료)
                .build();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
        configuration.setDatabase(database);
        return new LettuceConnectionFactory(configuration, lettuceClientConfiguration);
    }

    // RedisConnection에서 넘겨준 byte값 객체 직렬화
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // ConnectionFactory 설정.
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());

        ObjectMapper objectMapper = new ObjectMapper();
        // ObjectMapper에 JavaTimeModule 등록 (LocalDateTime 타입 직/역직렬화 할 수 있도록 하기 위해 설정)
        objectMapper.registerModule(new JavaTimeModule());
        // 객체를 메모리에 저장할 때 타입 정보를 포함시키도록 설정
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // key, value 직렬화 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        // hash key, value 직렬화 설정
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        return redisTemplate;
    }
}
