package com.buildflow.auth.repository;

import com.buildflow.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
    extends JpaRepository<
        PasswordResetToken,
        UUID
    > {

    Optional<PasswordResetToken>
        findByTokenHash(
            String tokenHash
        );

    void deleteByUserId(
        UUID userId
    );
}
