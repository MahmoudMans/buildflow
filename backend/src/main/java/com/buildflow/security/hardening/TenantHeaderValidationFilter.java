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
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantHeaderValidationFilter
    extends OncePerRequestFilter {

    private static final Pattern
        CANONICAL_UUID =
            Pattern.compile(
                "^[0-9a-fA-F]{8}-"
                    + "[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{12}$"
            );

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
        ) {
            filterChain.doFilter(
                request,
                response
            );

            return;
        }

        String organizationId =
            request.getHeader(
                "X-Organization-Id"
            );

        if (
            organizationId != null
            && (
                organizationId.isBlank()
                || !CANONICAL_UUID
                    .matcher(
                        organizationId
                    )
                    .matches()
            )
        ) {
            SecurityFilterResponses.write(
                request,
                response,
                HttpServletResponse
                    .SC_BAD_REQUEST,
                "INVALID_ORGANIZATION_ID",
                "X-Organization-Id must contain a canonical UUID."
            );

            return;
        }

        filterChain.doFilter(
            request,
            response
        );
    }
}
