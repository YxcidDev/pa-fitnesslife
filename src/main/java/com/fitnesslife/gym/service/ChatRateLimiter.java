package com.fitnesslife.gym.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fitnesslife.gym.exception.RateLimitExceededException;

@Service
public class ChatRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ChatRateLimiter.class);
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final int maxPerMinute;

    public ChatRateLimiter(
            StringRedisTemplate redis,
            @Value("${chatbot.rate-limit.max-per-minute:10}") int maxPerMinute) {
        this.redis = redis;
        this.maxPerMinute = maxPerMinute;
    }

    public void check(String userKey) {
        String key = "chatbot:rl:" + userKey;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return;
            }
            if (count == 1L) {
                redis.expire(key, WINDOW);
            }
            if (count > maxPerMinute) {
                Long ttl = redis.getExpire(key);
                if (ttl != null && ttl == -1L) {
                    redis.expire(key, WINDOW);
                }
                throw new RateLimitExceededException();
            }
        } catch (DataAccessException e) {
            log.warn("Rate limiter no disponible (Redis), se permite la petición: {}", e.getMessage());
        }
    }
}
