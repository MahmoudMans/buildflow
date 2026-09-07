package com.buildflow.auth.repository;

import com.buildflow.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository
    extends JpaRepository<AuthSession, UUID> {

    Optional<AuthSession>
        findByAccessTokenHashAndRevokedAtIsNull(
            String accessTokenHash
        );

    Optional<AuthSession>
        findByRefreshTokenHashAndRevokedAtIsNull(
            String refreshTokenHash
        );

    List<AuthSession>
        findByUserIdOrderByCreatedAtDesc(
            UUID userId
        );

    Optional<AuthSession>
        findByIdAndUserId(
            UUID id,
            UUID userId
        );

    @Modifying
    @Query("""
        update AuthSession session
        set session.revokedAt = :now
        where session.user.id = :userId
          and session.revokedAt is null
        """)
    int revokeAllForUser(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );
}
