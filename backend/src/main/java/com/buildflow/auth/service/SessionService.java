package com.buildflow.auth.service;

import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.entity.AuthSession;
import com.buildflow.auth.entity.UserStatus;
import com.buildflow.auth.repository.AuthSessionRepository;
import com.buildflow.security.SecurityProperties;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private final AuthSessionRepository repository;
    private final TokenCodec tokenCodec;
    private final SecurityProperties properties;

    public SessionService(
        AuthSessionRepository repository,
        TokenCodec tokenCodec,
        SecurityProperties properties
    ) {
        this.repository = repository;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
    }

    @Transactional
    public IssuedSession issue(
        AppUser user,
        String ipAddress,
        String userAgent
    ) {
        Instant now = Instant.now();

        String accessToken =
            tokenCodec.generate();

        String refreshToken =
            tokenCodec.generate();

        Instant accessExpiresAt =
            now.plus(
                properties.accessTokenTtl()
            );

        Instant refreshExpiresAt =
            now.plus(
                properties.refreshTokenTtl()
            );

        AuthSession session =
            AuthSession.create(
                user,
                tokenCodec.hash(accessToken),
                tokenCodec.hash(refreshToken),
                accessExpiresAt,
                refreshExpiresAt,
                now,
                normalizeIp(ipAddress),
                normalizeUserAgent(
                    userAgent
                )
            );

        repository.save(session);

        return new IssuedSession(
            session.getId(),
            accessToken,
            refreshToken,
            accessExpiresAt,
            refreshExpiresAt
        );
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedSession>
        authenticate(
            String accessToken
        ) {

        if (
            accessToken == null
                || accessToken.isBlank()
        ) {
            return Optional.empty();
        }

        Instant now = Instant.now();

        return repository
            .findByAccessTokenHashAndRevokedAtIsNull(
                tokenCodec.hash(
                    accessToken
                )
            )
            .filter(
                session ->
                    session.isAccessValid(
                        now
                    )
            )
            .filter(
                session ->
                    session
                        .getUser()
                        .getStatus()
                        == UserStatus.ACTIVE
            )
            .map(
                session ->
                    new AuthenticatedSession(
                        session.getId(),
                        session.getUser()
                    )
            );
    }

    @Transactional
    public IssuedSession refresh(
        String refreshToken,
        String ipAddress,
        String userAgent
    ) {
        if (
            refreshToken == null
                || refreshToken.isBlank()
        ) {
            throw unauthorizedSession();
        }

        Instant now = Instant.now();

        AuthSession session =
            repository
                .findByRefreshTokenHashAndRevokedAtIsNull(
                    tokenCodec.hash(
                        refreshToken
                    )
                )
                .orElseThrow(
                    this::unauthorizedSession
                );

        if (
            !session.isRefreshValid(now)
                || session
                    .getUser()
                    .getStatus()
                    != UserStatus.ACTIVE
        ) {
            session.revoke(now);

            throw unauthorizedSession();
        }

        String newAccessToken =
            tokenCodec.generate();

        String newRefreshToken =
            tokenCodec.generate();

        Instant accessExpiresAt =
            now.plus(
                properties.accessTokenTtl()
            );

        Instant refreshExpiresAt =
            now.plus(
                properties.refreshTokenTtl()
            );

        session.rotate(
            tokenCodec.hash(
                newAccessToken
            ),
            tokenCodec.hash(
                newRefreshToken
            ),
            accessExpiresAt,
            refreshExpiresAt,
            now,
            normalizeIp(ipAddress),
            normalizeUserAgent(userAgent)
        );

        return new IssuedSession(
            session.getId(),
            newAccessToken,
            newRefreshToken,
            accessExpiresAt,
            refreshExpiresAt
        );
    }

    @Transactional
    public void logout(
        String accessToken,
        String refreshToken
    ) {
        Instant now = Instant.now();

        if (
            accessToken != null
                && !accessToken.isBlank()
        ) {
            repository
                .findByAccessTokenHashAndRevokedAtIsNull(
                    tokenCodec.hash(
                        accessToken
                    )
                )
                .ifPresent(
                    session ->
                        session.revoke(now)
                );
        }

        if (
            refreshToken != null
                && !refreshToken.isBlank()
        ) {
            repository
                .findByRefreshTokenHashAndRevokedAtIsNull(
                    tokenCodec.hash(
                        refreshToken
                    )
                )
                .ifPresent(
                    session ->
                        session.revoke(now)
                );
        }
    }

    @Transactional
    public void revokeAllForUser(
        UUID userId
    ) {
        repository.revokeAllForUser(
            userId,
            Instant.now()
        );
    }

    @Transactional
    public boolean revokeSession(
        UUID userId,
        UUID sessionId
    ) {
        AuthSession session =
            repository
                .findByIdAndUserId(
                    sessionId,
                    userId
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "SESSION_NOT_FOUND",
                            "The session does not exist."
                        )
                );

        boolean wasActive =
            session.getRevokedAt()
                == null;

        session.revoke(
            Instant.now()
        );

        return wasActive;
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listSessions(
        UUID userId
    ) {
        return repository
            .findByUserIdOrderByCreatedAtDesc(
                userId
            );
    }

    private BusinessException unauthorizedSession() {
        return new BusinessException(
            HttpStatus.UNAUTHORIZED,
            "INVALID_SESSION",
            "The authentication session is invalid or expired."
        );
    }

    private String normalizeIp(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return limit(value, 100);
    }

    private String normalizeUserAgent(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return limit(value, 500);
    }

    private String limit(
        String value,
        int maximum
    ) {
        if (
            value.length()
                <= maximum
        ) {
            return value;
        }

        return value.substring(
            0,
            maximum
        );
    }

    public record IssuedSession(
        UUID sessionId,
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
    ) {
    }

    public record AuthenticatedSession(
        UUID sessionId,
        AppUser user
    ) {
    }
}
