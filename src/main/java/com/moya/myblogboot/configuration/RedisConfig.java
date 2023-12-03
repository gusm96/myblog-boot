package com.moya.myblogboot.configuration;

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
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Getter
@Configuration
@RequiredArgsConstructor
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.timeout}")
    private int timeout;
    private static final String REDISSON_HOST_PREFIX = "redis://";

    // Redis 연결
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory (){
        final SocketOptions socketoptions = SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build(); // 연결 시간제한.
        final ClientOptions clientoptions = ClientOptions.builder().socketOptions(socketoptions).build();
        LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
                .clientOptions(clientoptions)
                .commandTimeout(Duration.ofMinutes(1)) // 명령 최대시간.
                .shutdownTimeout(Duration.ZERO) // Redis 연결 종료시 타임아웃 설정. (강제 종료)
                .build();
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
        configuration.setDatabase(0); // 0번 DB로 설정.
        return new LettuceConnectionFactory(configuration,lettuceClientConfiguration);
    }

    // RedisConnection에서 넘겨준 byte값 객체 직렬화
    @Bean
    public RedisTemplate<?,?> redisTemplate(){
        GenericJackson2JsonRedisSerializer genericJackson2JsonSerializer = new GenericJackson2JsonRedisSerializer();
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // ConnectionFactory 설정.
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());

        // key, value 직렬화 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(genericJackson2JsonSerializer);

        // hash key, value 직렬화 설정
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(genericJackson2JsonSerializer);

        return redisTemplate;
    }
}
