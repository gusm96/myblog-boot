package com.moya.myblogboot.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Getter
@Configuration
@RequiredArgsConstructor
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.cache.redis.host}")
    private String host;

    @Value("${spring.cache.redis.port}")
    private int port;

    // Redis 연결
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory (){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(configuration);
    }

    // RedisConnection에서 넘겨준 byte값 객체 직렬화
    @Bean
    public RedisTemplate<?,?> redisTemplate(){
        RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory()); // RedisTemplate에 Redis 연결 팩토리를 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer()); // Redis key는 일반적으로 문자열로 사용되기 때문에 문자열로 직렬화
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer()); // Object로 직렬화
        return redisTemplate;
    }

}
