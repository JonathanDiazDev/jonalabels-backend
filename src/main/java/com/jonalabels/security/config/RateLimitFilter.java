package com.jonalabels.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String POST_COTIZACIONES = "/api/v1/cotizaciones";
    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.max-requests:30}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (shouldRateLimit(request) && isRateLimited(resolveClientKey(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"Too Many Requests","message":"Demasiadas solicitudes. Intenta de nuevo en un momento."}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRateLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && POST_COTIZACIONES.equals(path)) {
            return true;
        }

        return "POST".equals(method) && path.startsWith(AUTH_PREFIX);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() + ":" + request.getRequestURI();
    }

    private boolean isRateLimited(String key) {
        long now = Instant.now().toEpochMilli();
        WindowCounter counter = counters.compute(key, (existingKey, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                return new WindowCounter(now, 1);
            }
            existing.count++;
            return existing;
        });
        return counter.count > maxRequests;
    }

    private static final class WindowCounter {
        private final long windowStart;
        private int count;

        private WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
