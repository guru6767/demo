package com.starto.filter;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(30, Duration.ofMinutes(1)))
                .build();
    }

    private Bucket createStrictBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        // use Firebase UID if authenticated, fallback to IP
        String authHeader = req.getHeader("Authorization");
        String identifier;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // authenticated user — use token hash as key
            identifier = "user_" + Math.abs(authHeader.substring(7).hashCode());
        } else {
            // unauthenticated — use IP, stricter limit
            identifier = "ip_" + req.getRemoteAddr();
        }

        Bucket bucket = cache.computeIfAbsent(identifier, k ->
                identifier.startsWith("user_") ? createNewBucket() : createStrictBucket()
        );

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"Too many requests. Please slow down.\"}");
        }
    }
}