package com.buildflow.security.hardening;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(
    prefix = "buildflow.security"
)
public class SecurityHardeningProperties {

    private int loginMaxRequests =
        20;

    private long loginWindowSeconds =
        60L;

    private long maxRequestBodyBytes =
        27_262_976L;

    private String allowedOrigin =
        "http://localhost:3001";

    public int getLoginMaxRequests() {
        return loginMaxRequests;
    }

    public void setLoginMaxRequests(
        int loginMaxRequests
    ) {
        this.loginMaxRequests =
            Math.max(
                1,
                loginMaxRequests
            );
    }

    public long getLoginWindowSeconds() {
        return loginWindowSeconds;
    }

    public void setLoginWindowSeconds(
        long loginWindowSeconds
    ) {
        this.loginWindowSeconds =
            Math.max(
                1L,
                loginWindowSeconds
            );
    }

    public long getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    public void setMaxRequestBodyBytes(
        long maxRequestBodyBytes
    ) {
        this.maxRequestBodyBytes =
            Math.max(
                1L,
                maxRequestBodyBytes
            );
    }

    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    public void setAllowedOrigin(
        String allowedOrigin
    ) {
        if (
            allowedOrigin == null
            || allowedOrigin.isBlank()
        ) {
            return;
        }

        this.allowedOrigin =
            allowedOrigin.trim();
    }
}
