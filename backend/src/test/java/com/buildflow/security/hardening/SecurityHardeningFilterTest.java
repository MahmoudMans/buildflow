package com.buildflow.security.hardening;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHardeningFilterTest {

    @Test
    void addsSecurityResponseHeaders()
        throws Exception {

        SecurityHeadersFilter filter =
            new SecurityHeadersFilter();

        MockHttpServletRequest request =
            apiRequest(
                "GET",
                "/api/v1/projects"
            );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getHeader(
                "X-Content-Type-Options"
            )
        ).isEqualTo(
            "nosniff"
        );

        assertThat(
            response.getHeader(
                "X-Frame-Options"
            )
        ).isEqualTo(
            "DENY"
        );

        assertThat(
            response.getHeader(
                "Referrer-Policy"
            )
        ).isEqualTo(
            "no-referrer"
        );

        assertThat(
            response.getHeader(
                "Content-Security-Policy"
            )
        ).contains(
            "frame-ancestors 'none'"
        );

        assertThat(
            response.getHeader(
                "Cache-Control"
            )
        ).isEqualTo(
            "no-store"
        );
    }

    @Test
    void rejectsKnownOversizedRequest()
        throws Exception {

        SecurityHardeningProperties
            properties =
                properties();

        properties.setMaxRequestBodyBytes(
            512
        );

        RequestBodyLimitFilter filter =
            new RequestBodyLimitFilter(
                properties
            );

        MockHttpServletRequest request =
            apiRequest(
                "POST",
                "/api/v1/projects"
            );

        request.setContent(
            new byte[1024]
        );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getStatus()
        ).isEqualTo(
            413
        );

        assertThat(
            response
                .getContentAsString()
        ).contains(
            "REQUEST_TOO_LARGE"
        );
    }

    @Test
    void rejectsMalformedTenantHeader()
        throws Exception {

        TenantHeaderValidationFilter filter =
            new TenantHeaderValidationFilter();

        MockHttpServletRequest request =
            apiRequest(
                "GET",
                "/api/v1/projects"
            );

        request.addHeader(
            "X-Organization-Id",
            "not-a-uuid"
        );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getStatus()
        ).isEqualTo(
            400
        );

        assertThat(
            response
                .getContentAsString()
        ).contains(
            "INVALID_ORGANIZATION_ID"
        );
    }

    @Test
    void acceptsCanonicalTenantHeader()
        throws Exception {

        TenantHeaderValidationFilter filter =
            new TenantHeaderValidationFilter();

        MockHttpServletRequest request =
            apiRequest(
                "GET",
                "/api/v1/projects"
            );

        request.addHeader(
            "X-Organization-Id",
            UUID.randomUUID()
                .toString()
        );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getStatus()
        ).isEqualTo(
            200
        );
    }

    @Test
    void rejectsCrossSiteMutation()
        throws Exception {

        OriginMutationGuardFilter filter =
            new OriginMutationGuardFilter(
                properties()
            );

        MockHttpServletRequest request =
            apiRequest(
                "POST",
                "/api/v1/projects"
            );

        request.addHeader(
            "Origin",
            "https://evil.example"
        );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getStatus()
        ).isEqualTo(
            403
        );

        assertThat(
            response
                .getContentAsString()
        ).contains(
            "CROSS_SITE_MUTATION_REJECTED"
        );
    }

    @Test
    void rejectsSecFetchCrossSiteMutation()
        throws Exception {

        OriginMutationGuardFilter filter =
            new OriginMutationGuardFilter(
                properties()
            );

        MockHttpServletRequest request =
            apiRequest(
                "POST",
                "/api/v1/projects"
            );

        request.addHeader(
            "Sec-Fetch-Site",
            "cross-site"
        );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getStatus()
        ).isEqualTo(
            403
        );
    }

    @Test
    void acceptsConfiguredFrontendOrigin()
        throws Exception {

        OriginMutationGuardFilter filter =
            new OriginMutationGuardFilter(
                properties()
            );

        MockHttpServletRequest request =
            apiRequest(
                "POST",
                "/api/v1/projects"
            );

        request.addHeader(
            "Origin",
            "http://localhost:3001"
        );

        MockHttpServletResponse response =
            invoke(
                filter,
                request
            );

        assertThat(
            response.getStatus()
        ).isEqualTo(
            200
        );
    }

    @Test
    void loginRateLimitCannotBeEvadedWithForwardedHeader()
        throws Exception {

        SecurityHardeningProperties
            properties =
                properties();

        properties.setLoginMaxRequests(
            2
        );

        properties.setLoginWindowSeconds(
            60
        );

        LoginRateLimitFilter filter =
            new LoginRateLimitFilter(
                properties
            );

        MockHttpServletRequest first =
            loginRequest(
                "203.0.113.10"
            );

        first.addHeader(
            "X-Forwarded-For",
            "198.51.100.1"
        );

        MockHttpServletResponse firstResponse =
            invoke(
                filter,
                first
            );

        assertThat(
            firstResponse.getStatus()
        ).isEqualTo(
            200
        );

        assertThat(
            firstResponse.getHeader(
                "X-RateLimit-Remaining"
            )
        ).isEqualTo(
            "1"
        );

        MockHttpServletRequest second =
            loginRequest(
                "203.0.113.10"
            );

        second.addHeader(
            "X-Forwarded-For",
            "198.51.100.2"
        );

        MockHttpServletResponse secondResponse =
            invoke(
                filter,
                second
            );

        assertThat(
            secondResponse.getStatus()
        ).isEqualTo(
            200
        );

        assertThat(
            secondResponse.getHeader(
                "X-RateLimit-Remaining"
            )
        ).isEqualTo(
            "0"
        );

        MockHttpServletRequest third =
            loginRequest(
                "203.0.113.10"
            );

        third.addHeader(
            "X-Forwarded-For",
            "198.51.100.250"
        );

        MockHttpServletResponse thirdResponse =
            invoke(
                filter,
                third
            );

        assertThat(
            thirdResponse.getStatus()
        ).isEqualTo(
            429
        );

        assertThat(
            thirdResponse.getHeader(
                "X-RateLimit-Remaining"
            )
        ).isEqualTo(
            "0"
        );

        assertThat(
            thirdResponse.getHeader(
                "Retry-After"
            )
        ).isNotBlank();

        assertThat(
            thirdResponse
                .getContentAsString()
        ).contains(
            "RATE_LIMIT_EXCEEDED"
        );
    }

    private SecurityHardeningProperties
        properties() {

        SecurityHardeningProperties
            properties =
                new SecurityHardeningProperties();

        properties.setAllowedOrigin(
            "http://localhost:3001"
        );

        return properties;
    }

    private MockHttpServletRequest
        apiRequest(
            String method,
            String path
        ) {

        MockHttpServletRequest request =
            new MockHttpServletRequest();

        request.setMethod(method);
        request.setRequestURI(path);
        request.setScheme("http");
        request.setServerName(
            "localhost"
        );
        request.setServerPort(
            8080
        );
        request.setRemoteAddr(
            "127.0.0.1"
        );

        return request;
    }

    private MockHttpServletRequest
        loginRequest(
            String remoteAddress
        ) {

        MockHttpServletRequest request =
            apiRequest(
                "POST",
                "/api/v1/auth/login"
            );

        request.setRemoteAddr(
            remoteAddress
        );

        request.setContentType(
            "application/json"
        );

        request.setContent(
            "{}".getBytes(
                StandardCharsets.UTF_8
            )
        );

        return request;
    }

    private MockHttpServletResponse invoke(
        jakarta.servlet.Filter filter,
        MockHttpServletRequest request
    ) throws Exception {

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        filter.doFilter(
            request,
            response,
            new MockFilterChain()
        );

        return response;
    }
}
