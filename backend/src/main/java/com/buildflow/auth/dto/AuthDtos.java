package com.buildflow.auth.dto;

import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.entity.AuthSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(
            min = 12,
            max = 72
        )
        String password

    ) {
    }

    public record LoginRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @Size(max = 72)
        String password

    ) {
    }

    public record ForgotPasswordRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email

    ) {
    }

    public record ResetPasswordRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(
            min = 12,
            max = 72
        )
        String newPassword

    ) {
    }

    public record VerifyEmailRequest(

        @NotBlank
        String token

    ) {
    }

    public record ChangePasswordRequest(

        @NotBlank
        @Size(max = 72)
        String currentPassword,

        @NotBlank
        @Size(
            min = 12,
            max = 72
        )
        String newPassword

    ) {
    }

    public record UserView(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified,
        Instant createdAt
    ) {
        public static UserView from(
            AppUser user
        ) {
            return new UserView(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEmailVerified(),
                user.getCreatedAt()
            );
        }
    }

    public record CsrfView(
        String headerName,
        String token
    ) {
    }

    public record SessionView(
        UUID id,
        boolean current,
        Instant createdAt,
        Instant lastUsedAt,
        Instant refreshExpiresAt,
        Instant revokedAt,
        String ipAddress,
        String userAgent
    ) {
        public static SessionView from(
            AuthSession session,
            UUID currentSessionId
        ) {
            return new SessionView(
                session.getId(),
                session
                    .getId()
                    .equals(
                        currentSessionId
                    ),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getRefreshExpiresAt(),
                session.getRevokedAt(),
                session.getIpAddress(),
                session.getUserAgent()
            );
        }
    }
}
