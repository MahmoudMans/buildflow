package com.buildflow.auth.service;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.entity.EmailVerificationToken;
import com.buildflow.auth.entity.PasswordResetToken;
import com.buildflow.auth.entity.UserStatus;
import com.buildflow.auth.repository.AppUserRepository;
import com.buildflow.auth.repository.EmailVerificationTokenRepository;
import com.buildflow.auth.repository.PasswordResetTokenRepository;
import com.buildflow.security.SecurityProperties;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final EmailVerificationTokenRepository
        verificationRepository;
    private final PasswordResetTokenRepository
        resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCodec tokenCodec;
    private final SessionService sessionService;
    private final ApplicationMailService mailService;
    private final SecurityProperties properties;
    private final LoginRateLimiter rateLimiter;

    private final String dummyPasswordHash;

    public AuthService(
        AppUserRepository userRepository,
        EmailVerificationTokenRepository
            verificationRepository,
        PasswordResetTokenRepository
            resetRepository,
        PasswordEncoder passwordEncoder,
        TokenCodec tokenCodec,
        SessionService sessionService,
        ApplicationMailService mailService,
        SecurityProperties properties,
        LoginRateLimiter rateLimiter
    ) {
        this.userRepository = userRepository;
        this.verificationRepository =
            verificationRepository;
        this.resetRepository =
            resetRepository;
        this.passwordEncoder =
            passwordEncoder;
        this.tokenCodec = tokenCodec;
        this.sessionService =
            sessionService;
        this.mailService = mailService;
        this.properties = properties;
        this.rateLimiter = rateLimiter;

        this.dummyPasswordHash =
            passwordEncoder.encode(
                UUID.randomUUID()
                    .toString()
            );
    }

    @Transactional
    public AuthResult register(
        AuthDtos.RegisterRequest request,
        String ipAddress,
        String userAgent
    ) {
        String email =
            normalizeEmail(
                request.email()
            );

        if (
            userRepository
                .existsByEmailIgnoreCase(
                    email
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "An account already exists for this email address."
            );
        }

        Instant now = Instant.now();

        AppUser user =
            AppUser.create(
                email,
                passwordEncoder.encode(
                    request.password()
                ),
                cleanRequired(
                    request.firstName()
                ),
                cleanRequired(
                    request.lastName()
                ),
                now
            );

        userRepository.save(user);

        issueVerificationToken(
            user,
            now
        );

        SessionService.IssuedSession session =
            sessionService.issue(
                user,
                ipAddress,
                userAgent
            );

        return new AuthResult(
            user,
            session
        );
    }

    @Transactional
    public AuthResult login(
        AuthDtos.LoginRequest request,
        String ipAddress,
        String userAgent
    ) {
        String email =
            normalizeEmail(
                request.email()
            );

        String rateLimitKey =
            email
                + "|"
                + String.valueOf(
                    ipAddress
                );

        rateLimiter.check(
            rateLimitKey
        );

        AppUser user =
            userRepository
                .findByEmailIgnoreCase(
                    email
                )
                .orElse(null);

        if (user == null) {
            passwordEncoder.matches(
                request.password(),
                dummyPasswordHash
            );

            rateLimiter.failure(
                rateLimitKey
            );

            throw invalidCredentials();
        }

        if (
            !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
            )
        ) {
            rateLimiter.failure(
                rateLimitKey
            );

            throw invalidCredentials();
        }

        if (
            user.getStatus()
                != UserStatus.ACTIVE
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED",
                "This account is disabled."
            );
        }

        rateLimiter.success(
            rateLimitKey
        );

        SessionService.IssuedSession session =
            sessionService.issue(
                user,
                ipAddress,
                userAgent
            );

        return new AuthResult(
            user,
            session
        );
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(
        UUID userId
    ) {
        return userRepository
            .findById(userId)
            .filter(
                user ->
                    user.getStatus()
                        == UserStatus.ACTIVE
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_NOT_AVAILABLE",
                        "The user account is unavailable."
                    )
            );
    }

    @Transactional
    public void requestPasswordReset(
        AuthDtos.ForgotPasswordRequest request
    ) {
        String email =
            normalizeEmail(
                request.email()
            );

        userRepository
            .findByEmailIgnoreCase(
                email
            )
            .filter(
                user ->
                    user.getStatus()
                        == UserStatus.ACTIVE
            )
            .ifPresent(
                user -> {
                    resetRepository
                        .deleteByUserId(
                            user.getId()
                        );

                    Instant now =
                        Instant.now();

                    String rawToken =
                        tokenCodec.generate();

                    PasswordResetToken token =
                        PasswordResetToken.create(
                            user,
                            tokenCodec.hash(
                                rawToken
                            ),
                            now.plus(
                                properties
                                    .passwordResetTtl()
                            ),
                            now
                        );

                    resetRepository.save(
                        token
                    );

                    mailService
                        .sendPasswordReset(
                            user.getEmail(),
                            rawToken
                        );
                }
            );
    }

    @Transactional
    public void resetPassword(
        AuthDtos.ResetPasswordRequest request
    ) {
        Instant now = Instant.now();

        PasswordResetToken token =
            resetRepository
                .findByTokenHash(
                    tokenCodec.hash(
                        request.token()
                    )
                )
                .orElseThrow(
                    this::invalidResetToken
                );

        if (
            !token.isUsable(now)
        ) {
            throw invalidResetToken();
        }

        AppUser user =
            token.getUser();

        user.changePassword(
            passwordEncoder.encode(
                request.newPassword()
            ),
            now
        );

        token.markUsed(now);

        sessionService
            .revokeAllForUser(
                user.getId()
            );
    }

    @Transactional
    public void verifyEmail(
        AuthDtos.VerifyEmailRequest request
    ) {
        Instant now = Instant.now();

        EmailVerificationToken token =
            verificationRepository
                .findByTokenHash(
                    tokenCodec.hash(
                        request.token()
                    )
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            "INVALID_VERIFICATION_TOKEN",
                            "The verification token is invalid or expired."
                        )
                );

        if (
            !token.isUsable(now)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_VERIFICATION_TOKEN",
                "The verification token is invalid or expired."
            );
        }

        token.getUser()
            .verifyEmail(now);

        token.markUsed(now);
    }

    @Transactional
    public void resendVerification(
        UUID userId
    ) {
        AppUser user =
            requireUser(userId);

        if (
            user.isEmailVerified()
        ) {
            return;
        }

        Instant now = Instant.now();

        issueVerificationToken(
            user,
            now
        );
    }

    @Transactional
    public AuthResult changePassword(
        UUID userId,
        AuthDtos.ChangePasswordRequest
            request,
        String ipAddress,
        String userAgent
    ) {
        AppUser user =
            requireUser(userId);

        if (
            !passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
            )
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CURRENT_PASSWORD_INVALID",
                "The current password is incorrect."
            );
        }

        Instant now = Instant.now();

        user.changePassword(
            passwordEncoder.encode(
                request.newPassword()
            ),
            now
        );

        sessionService
            .revokeAllForUser(
                user.getId()
            );

        SessionService.IssuedSession session =
            sessionService.issue(
                user,
                ipAddress,
                userAgent
            );

        return new AuthResult(
            user,
            session
        );
    }

    private void issueVerificationToken(
        AppUser user,
        Instant now
    ) {
        verificationRepository
            .deleteByUserId(
                user.getId()
            );

        String rawToken =
            tokenCodec.generate();

        EmailVerificationToken token =
            EmailVerificationToken.create(
                user,
                tokenCodec.hash(
                    rawToken
                ),
                now.plus(
                    properties
                        .emailVerificationTtl()
                ),
                now
            );

        verificationRepository.save(
            token
        );

        mailService
            .sendEmailVerification(
                user.getEmail(),
                rawToken
            );
    }

    private BusinessException
        invalidCredentials() {

        return new BusinessException(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            "The email or password is incorrect."
        );
    }

    private BusinessException
        invalidResetToken() {

        return new BusinessException(
            HttpStatus.BAD_REQUEST,
            "INVALID_PASSWORD_RESET_TOKEN",
            "The password reset token is invalid or expired."
        );
    }

    private String normalizeEmail(
        String value
    ) {
        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private String cleanRequired(
        String value
    ) {
        return value.trim();
    }

    public record AuthResult(
        AppUser user,
        SessionService.IssuedSession session
    ) {
    }
}
