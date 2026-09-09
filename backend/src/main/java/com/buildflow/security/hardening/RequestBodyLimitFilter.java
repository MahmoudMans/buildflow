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
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBodyLimitFilter
    extends OncePerRequestFilter {

    private static final Set<String>
        BODY_METHODS =
            Set.of(
                "POST",
                "PUT",
                "PATCH"
            );

    private final SecurityHardeningProperties
        properties;

    public RequestBodyLimitFilter(
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
            !request.getRequestURI()
                .startsWith(
                    "/api/"
                )
            || !BODY_METHODS.contains(
                request.getMethod()
            )
        ) {
            filterChain.doFilter(
                request,
                response
            );

            return;
        }

        long contentLength =
            request.getContentLengthLong();

        if (
            contentLength
                > properties
                    .getMaxRequestBodyBytes()
        ) {
            SecurityFilterResponses.write(
                request,
                response,
                HttpServletResponse
                    .SC_REQUEST_ENTITY_TOO_LARGE,
                "REQUEST_TOO_LARGE",
                "The request body exceeds the configured size limit."
            );

            return;
        }

        filterChain.doFilter(
            request,
            response
        );
    }
}
