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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter
    extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        response.setHeader(
            "X-Content-Type-Options",
            "nosniff"
        );

        response.setHeader(
            "X-Frame-Options",
            "DENY"
        );

        response.setHeader(
            "Referrer-Policy",
            "no-referrer"
        );

        response.setHeader(
            "Permissions-Policy",
            "camera=(), microphone=(), geolocation=()"
        );

        response.setHeader(
            "Cross-Origin-Resource-Policy",
            "same-site"
        );

        if (
            request.getRequestURI()
                .startsWith(
                    "/api/"
                )
            || request.getRequestURI()
                .startsWith(
                    "/actuator/"
                )
        ) {
            response.setHeader(
                "Cache-Control",
                "no-store"
            );

            response.setHeader(
                "Pragma",
                "no-cache"
            );

            response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; "
                    + "frame-ancestors 'none'; "
                    + "base-uri 'none'"
            );
        }

        if (request.isSecure()) {
            response.setHeader(
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains"
            );
        }

        filterChain.doFilter(
            request,
            response
        );
    }
}
