package com.example.demo.filter;

import com.example.demo.config.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter limiter;
    private final boolean enabled;
    private final int capacity;
    private final double refillPerSecond;

    public RateLimitFilter(RateLimiter limiter,
                           @Value("${app.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.rate-limit.capacity}") int capacity,
                           @Value("${app.rate-limit.refill-per-second}") double refillPerSecond) {
        this.limiter = limiter;
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean allowed;
        try {
            allowed = limiter.tryAcquire(request.getRemoteAddr(), capacity, refillPerSecond);
        } catch (Exception e) {
            log.warn("Rate limiter unavailable, letting the request through", e);
            allowed = true;      // fail-open: Redis lỗi thì KHÔNG chặn webhook thật
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "1");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limited\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
