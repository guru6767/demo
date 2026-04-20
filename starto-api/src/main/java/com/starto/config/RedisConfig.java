package com.starto.config;

import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.serializer.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisSerializer<Object> redisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // 🔥 IMPORTANT FIX: enable type information
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<Object> serializer = redisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet(); 

        return template;
    }

    @Bean
    public CacheManager cacheManager() {

        RedisSerializationContext.SerializationPair<Object> valueSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer());

        RedisSerializationContext.SerializationPair<String> keySerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

        RedisCacheConfiguration exploreConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .serializeKeysWith(keySerializer)
                .serializeValuesWith(valueSerializer);

        RedisCacheConfiguration userConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(keySerializer)
                .serializeValuesWith(valueSerializer);

        RedisCacheConfiguration signalConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .serializeKeysWith(keySerializer)
                .serializeValuesWith(valueSerializer);

        RedisCacheConfiguration reviewConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(keySerializer)
                .serializeValuesWith(valueSerializer);

        return RedisCacheManager.builder(connectionFactory())
                .cacheDefaults(exploreConfig)
                .withCacheConfiguration("userCache", userConfig)
                .withCacheConfiguration("signalCache", signalConfig)
                .withCacheConfiguration("reviewCache", reviewConfig)
                .build();
    }
}