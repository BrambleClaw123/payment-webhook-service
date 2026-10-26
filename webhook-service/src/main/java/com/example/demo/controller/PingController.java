package com.example.demo.controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Map;

@RestController
public class PingController {

    private final StringRedisTemplate redis;

    public PingController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Long count = redis.opsForValue().increment("ping:count");
        return Map.of("pong", true, "count", count);
    }
}