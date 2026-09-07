package com.buildflow.auth.repository;

import com.buildflow.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository
    extends JpaRepository<
        EmailVerificationToken,
        UUID
    > {

    Optional<EmailVerificationToken>
        findByTokenHash(
            String tokenHash
        );

    void deleteByUserId(
        UUID userId
    );
}
