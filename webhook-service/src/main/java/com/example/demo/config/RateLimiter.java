package com.example.demo.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RateLimiter {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> script;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        s.setResultType(Long.class);
        this.script = s;
    }

    public boolean tryAcquire(String clientKey, int capacity, double refillPerSecond) {
        Long result = redis.execute(script,
                List.of("rate:" + clientKey),
                String.valueOf(capacity),
                String.valueOf(refillPerSecond));
        return result != null && result == 1L;
    }
}
