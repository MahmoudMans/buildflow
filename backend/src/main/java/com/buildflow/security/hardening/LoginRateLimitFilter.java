package com.buildflow.security.hardening;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class LoginRateLimitFilter
    extends OncePerRequestFilter {

    private static final String
        LOGIN_PATH =
            "/api/v1/auth/login";

    private final SecurityHardeningProperties
        properties;

    private final Map<String, Bucket>
        buckets =
            new ConcurrentHashMap<>();

    public LoginRateLimitFilter(
        SecurityHardeningProperties properties
    ) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (
            !"POST".equals(
                request.getMethod()
            )
            || !LOGIN_PATH.equals(
                request.getRequestURI()
            )
        ) {
            filterChain.doFilter(
                request,
                response
            );

            return;
        }

        long now =
            Instant.now()
                .getEpochSecond();

        long windowSeconds =
            properties
                .getLoginWindowSeconds();

        int limit =
            properties
                .getLoginMaxRequests();

        String key =
            clientKey(
                request
            );

        Bucket bucket =
            buckets.computeIfAbsent(
                key,
                ignored ->
                    new Bucket(
                        now
                    )
            );

        RateDecision decision;

        synchronized (bucket) {
            if (
                now - bucket.windowStart
                    >= windowSeconds
            ) {
                bucket.windowStart =
                    now;

                bucket.count = 0;
            }

            long resetAt =
                bucket.windowStart
                    + windowSeconds;

            if (
                bucket.count
                    >= limit
            ) {
                decision =
                    new RateDecision(
                        true,
                        0,
                        resetAt
                    );
            } else {
                bucket.count++;

                decision =
                    new RateDecision(
                        false,
                        Math.max(
                            0,
                            limit
                                - bucket.count
                        ),
                        resetAt
                    );
            }
        }

        response.setHeader(
            "X-RateLimit-Limit",
            String.valueOf(
                limit
            )
        );

        response.setHeader(
            "X-RateLimit-Remaining",
            String.valueOf(
                decision.remaining()
            )
        );

        response.setHeader(
            "X-RateLimit-Reset",
            String.valueOf(
                decision.resetAt()
            )
        );

        if (decision.rejected()) {
            long retryAfter =
                Math.max(
                    1L,
                    decision.resetAt()
                        - now
                );

            response.setHeader(
                "Retry-After",
                String.valueOf(
                    retryAfter
                )
            );

            SecurityFilterResponses.write(
                request,
                response,
                429,
                "RATE_LIMIT_EXCEEDED",
                "Too many login attempts. Try again later."
            );

            return;
        }

        cleanupOldBuckets(
            now,
            windowSeconds
        );

        filterChain.doFilter(
            request,
            response
        );
    }

    /*
     * Deliberately use the socket peer address.
     *
     * X-Forwarded-For and similar headers are user-controllable
     * unless a trusted reverse proxy boundary has explicitly
     * been configured. Trusting them here would let a client
     * evade the limiter by rotating spoofed header values.
     */
    private String clientKey(
        HttpServletRequest request
    ) {
        String remoteAddress =
            request.getRemoteAddr();

        if (
            remoteAddress == null
            || remoteAddress.isBlank()
        ) {
            return "unknown";
        }

        return remoteAddress.trim();
    }

    private void cleanupOldBuckets(
        long now,
        long windowSeconds
    ) {
        if (
            buckets.size()
                < 10_000
        ) {
            return;
        }

        long oldestAllowed =
            now - (
                windowSeconds * 2L
            );

        buckets.entrySet()
            .removeIf(
                entry ->
                    entry
                        .getValue()
                        .windowStart
                        < oldestAllowed
            );
    }

    private static final class Bucket {

        private long windowStart;

        private int count;

        private Bucket(
            long windowStart
        ) {
            this.windowStart =
                windowStart;
        }
    }

    private record RateDecision(
        boolean rejected,
        int remaining,
        long resetAt
    ) {
    }
}
