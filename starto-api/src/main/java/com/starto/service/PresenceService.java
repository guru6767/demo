package com.starto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private static final String PRESENCE_KEY_PREFIX = "presence:";

    public void markOnline(String userId, String city) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, city, Duration.ofSeconds(45));
    }

    public void markOffline(String userId) {
        redisTemplate.delete(PRESENCE_KEY_PREFIX + userId);
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId));
    }
}
