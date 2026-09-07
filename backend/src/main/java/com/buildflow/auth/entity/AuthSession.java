package com.buildflow.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    private UUID id;

    @ManyToOne(
        fetch = FetchType.EAGER,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private AppUser user;

    @Column(
        name = "access_token_hash",
        nullable = false,
        length = 64
    )
    private String accessTokenHash;

    @Column(
        name = "refresh_token_hash",
        nullable = false,
        length = 64
    )
    private String refreshTokenHash;

    @Column(
        name = "access_expires_at",
        nullable = false
    )
    private Instant accessExpiresAt;

    @Column(
        name = "refresh_expires_at",
        nullable = false
    )
    private Instant refreshExpiresAt;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    @Column(
        name = "last_used_at",
        nullable = false
    )
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(
        name = "ip_address",
        length = 100
    )
    private String ipAddress;

    @Column(
        name = "user_agent",
        length = 500
    )
    private String userAgent;

    @Version
    @Column(nullable = false)
    private long version;

    protected AuthSession() {
    }

    public static AuthSession create(
        AppUser user,
        String accessTokenHash,
        String refreshTokenHash,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        Instant now,
        String ipAddress,
        String userAgent
    ) {
        AuthSession session =
            new AuthSession();

        session.id = UUID.randomUUID();
        session.user = user;
        session.accessTokenHash = accessTokenHash;
        session.refreshTokenHash =
            refreshTokenHash;
        session.accessExpiresAt =
            accessExpiresAt;
        session.refreshExpiresAt =
            refreshExpiresAt;
        session.createdAt = now;
        session.lastUsedAt = now;
        session.ipAddress = ipAddress;
        session.userAgent = userAgent;

        return session;
    }

    public void rotate(
        String accessTokenHash,
        String refreshTokenHash,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        Instant now,
        String ipAddress,
        String userAgent
    ) {
        this.accessTokenHash =
            accessTokenHash;

        this.refreshTokenHash =
            refreshTokenHash;

        this.accessExpiresAt =
            accessExpiresAt;

        this.refreshExpiresAt =
            refreshExpiresAt;

        this.lastUsedAt = now;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public void revoke(
        Instant now
    ) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public boolean isAccessValid(
        Instant now
    ) {
        return revokedAt == null
            && accessExpiresAt.isAfter(now);
    }

    public boolean isRefreshValid(
        Instant now
    ) {
        return revokedAt == null
            && refreshExpiresAt.isAfter(now);
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Instant getAccessExpiresAt() {
        return accessExpiresAt;
    }

    public Instant getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
