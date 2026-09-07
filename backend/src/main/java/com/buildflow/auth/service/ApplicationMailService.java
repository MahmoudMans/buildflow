package com.buildflow.auth.service;

import com.buildflow.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ApplicationMailService {

    private static final Logger log =
        LoggerFactory.getLogger(
            ApplicationMailService.class
        );

    private final JavaMailSender mailSender;
    private final SecurityProperties properties;

    public ApplicationMailService(
        JavaMailSender mailSender,
        SecurityProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendEmailVerification(
        String email,
        String token
    ) {
        String link =
            properties.frontendUrl()
                + "/verify-email?token="
                + encode(token);

        send(
            email,
            "Verify your BuildFlow email",
            "Verify your BuildFlow email using this link:\n\n"
                + link
        );
    }

    public void sendPasswordReset(
        String email,
        String token
    ) {
        String link =
            properties.frontendUrl()
                + "/reset-password?token="
                + encode(token);

        send(
            email,
            "Reset your BuildFlow password",
            "Reset your BuildFlow password using this link:\n\n"
                + link
        );
    }

    public void sendInvitation(
        String email,
        String organizationName,
        String token
    ) {
        String link =
            properties.frontendUrl()
                + "/invitations/accept?token="
                + encode(token);

        send(
            email,
            "BuildFlow invitation",
            "You have been invited to join "
                + organizationName
                + " on BuildFlow.\n\n"
                + link
        );
    }

    private void send(
        String to,
        String subject,
        String body
    ) {
        try {
            SimpleMailMessage message =
                new SimpleMailMessage();

            message.setFrom(
                "no-reply@buildflow.local"
            );

            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (MailException exception) {
            log.warn(
                "Email delivery failed for {}: {}",
                to,
                exception.getMessage()
            );
        }
    }

    private String encode(
        String value
    ) {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8
        );
    }
}
