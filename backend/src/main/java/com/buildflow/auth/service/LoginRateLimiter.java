package com.buildflow.auth.service;

import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;

    private static final Duration WINDOW =
        Duration.ofMinutes(15);

    private final ConcurrentHashMap<
        String,
        Attempt
    > attempts =
        new ConcurrentHashMap<>();

    public void check(
        String key
    ) {
        Attempt attempt =
            attempts.get(key);

        if (attempt == null) {
            return;
        }

        Instant now = Instant.now();

        if (
            attempt.startedAt()
                .plus(WINDOW)
                .isBefore(now)
        ) {
            attempts.remove(key);
            return;
        }

        if (
            attempt.failures()
                >= MAX_FAILURES
        ) {
            throw new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "LOGIN_RATE_LIMITED",
                "Too many failed login attempts. Try again later."
            );
        }
    }

    public void failure(
        String key
    ) {
        Instant now = Instant.now();

        attempts.compute(
            key,
            (ignored, current) -> {
                if (
                    current == null
                        || current.startedAt()
                            .plus(WINDOW)
                            .isBefore(now)
                ) {
                    return new Attempt(
                        1,
                        now
                    );
                }

                return new Attempt(
                    current.failures() + 1,
                    current.startedAt()
                );
            }
        );
    }

    public void success(
        String key
    ) {
        attempts.remove(key);
    }

    private record Attempt(
        int failures,
        Instant startedAt
    ) {
    }
}
