package com.buildflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
    prefix = "app.security"
)
public record SecurityProperties(
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    Duration emailVerificationTtl,
    Duration passwordResetTtl,
    Duration invitationTtl,
    boolean cookieSecure,
    String frontendUrl
) {
}
