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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class OriginMutationGuardFilter
    extends OncePerRequestFilter {

    private static final Set<String>
        SAFE_METHODS =
            Set.of(
                "GET",
                "HEAD",
                "OPTIONS",
                "TRACE"
            );

    private final SecurityHardeningProperties
        properties;

    public OriginMutationGuardFilter(
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
                    "/api/v1/"
                )
            || SAFE_METHODS.contains(
                request.getMethod()
            )
        ) {
            filterChain.doFilter(
                request,
                response
            );

            return;
        }

        String fetchSite =
            request.getHeader(
                "Sec-Fetch-Site"
            );

        if (
            fetchSite != null
            && "cross-site".equalsIgnoreCase(
                fetchSite.trim()
            )
        ) {
            reject(
                request,
                response
            );

            return;
        }

        String origin =
            trimToNull(
                request.getHeader(
                    "Origin"
                )
            );

        if (
            origin != null
            && !allowed(
                origin,
                request
            )
        ) {
            reject(
                request,
                response
            );

            return;
        }

        if (origin == null) {
            String referer =
                trimToNull(
                    request.getHeader(
                        "Referer"
                    )
                );

            if (
                referer != null
                && !allowed(
                    referer,
                    request
                )
            ) {
                reject(
                    request,
                    response
                );

                return;
            }
        }

        filterChain.doFilter(
            request,
            response
        );
    }

    private void reject(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {

        SecurityFilterResponses.write(
            request,
            response,
            HttpServletResponse
                .SC_FORBIDDEN,
            "CROSS_SITE_MUTATION_REJECTED",
            "Cross-site state-changing requests are not permitted."
        );
    }

    private boolean allowed(
        String candidate,
        HttpServletRequest request
    ) {
        Origin supplied =
            parseOrigin(
                candidate
            );

        if (supplied == null) {
            return false;
        }

        Origin frontend =
            parseOrigin(
                properties
                    .getAllowedOrigin()
            );

        if (
            frontend != null
            && sameOrigin(
                supplied,
                frontend
            )
        ) {
            return true;
        }

        Origin backend =
            new Origin(
                request.getScheme(),
                normalizeHost(
                    request.getServerName()
                ),
                normalizePort(
                    request.getScheme(),
                    request.getServerPort()
                )
            );

        return sameOrigin(
            supplied,
            backend
        );
    }

    private Origin parseOrigin(
        String value
    ) {
        try {
            URI uri =
                new URI(value);

            if (
                uri.getScheme() == null
                || uri.getHost() == null
            ) {
                return null;
            }

            return new Origin(
                uri.getScheme(),
                normalizeHost(
                    uri.getHost()
                ),
                normalizePort(
                    uri.getScheme(),
                    uri.getPort()
                )
            );

        } catch (
            URISyntaxException exception
        ) {
            return null;
        }
    }

    private boolean sameOrigin(
        Origin first,
        Origin second
    ) {
        if (
            !first.scheme()
                .equalsIgnoreCase(
                    second.scheme()
                )
            || first.port()
                != second.port()
        ) {
            return false;
        }

        if (
            first.host()
                .equalsIgnoreCase(
                    second.host()
                )
        ) {
            return true;
        }

        return isLoopback(
            first.host()
        )
            && isLoopback(
                second.host()
            );
    }

    private boolean isLoopback(
        String host
    ) {
        return "localhost".equalsIgnoreCase(
            host
        )
            || "127.0.0.1".equals(
                host
            )
            || "0:0:0:0:0:0:0:1".equals(
                host
            )
            || "::1".equals(
                host
            );
    }

    private String normalizeHost(
        String host
    ) {
        if (host == null) {
            return "";
        }

        return host
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private int normalizePort(
        String scheme,
        int port
    ) {
        if (port >= 0) {
            return port;
        }

        if (
            "https".equalsIgnoreCase(
                scheme
            )
        ) {
            return 443;
        }

        if (
            "http".equalsIgnoreCase(
                scheme
            )
        ) {
            return 80;
        }

        return port;
    }

    private String trimToNull(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private record Origin(
        String scheme,
        String host,
        int port
    ) {
    }
}
