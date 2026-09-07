package com.buildflow.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    private UUID id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private AppUser user;

    @Column(
        name = "token_hash",
        nullable = false,
        length = 64
    )
    private String tokenHash;

    @Column(
        name = "expires_at",
        nullable = false
    )
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected EmailVerificationToken() {
    }

    public static EmailVerificationToken create(
        AppUser user,
        String tokenHash,
        Instant expiresAt,
        Instant now
    ) {
        EmailVerificationToken token =
            new EmailVerificationToken();

        token.id = UUID.randomUUID();
        token.user = user;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.createdAt = now;

        return token;
    }

    public boolean isUsable(
        Instant now
    ) {
        return usedAt == null
            && expiresAt.isAfter(now);
    }

    public void markUsed(
        Instant now
    ) {
        usedAt = now;
    }

    public AppUser getUser() {
        return user;
    }
}
