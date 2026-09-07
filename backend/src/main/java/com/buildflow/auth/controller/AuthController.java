package com.buildflow.auth.controller;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.service.AuthCookieService;
import com.buildflow.auth.service.AuthService;
import com.buildflow.auth.service.SessionService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final AuthCookieService
        cookieService;
    private final OrganizationService
        organizationService;

    public AuthController(
        AuthService authService,
        SessionService sessionService,
        AuthCookieService cookieService,
        OrganizationService
            organizationService
    ) {
        this.authService = authService;
        this.sessionService =
            sessionService;
        this.cookieService =
            cookieService;
        this.organizationService =
            organizationService;
    }

    @GetMapping("/csrf")
    public AuthDtos.CsrfView csrf(
        CsrfToken csrfToken
    ) {
        return new AuthDtos.CsrfView(
            csrfToken.getHeaderName(),
            csrfToken.getToken()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<
        AuthResponse
    > register(
        @Valid
        @RequestBody
        AuthDtos.RegisterRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        AuthService.AuthResult result =
            authService.register(
                request,
                servletRequest
                    .getRemoteAddr(),
                servletRequest
                    .getHeader(
                        "User-Agent"
                    )
            );

        cookieService.writeSession(
            servletResponse,
            result.session()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                responseFor(
                    result.user()
                )
            );
    }

    @PostMapping("/login")
    public AuthResponse login(
        @Valid
        @RequestBody
        AuthDtos.LoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        AuthService.AuthResult result =
            authService.login(
                request,
                servletRequest
                    .getRemoteAddr(),
                servletRequest
                    .getHeader(
                        "User-Agent"
                    )
            );

        cookieService.writeSession(
            servletResponse,
            result.session()
        );

        return responseFor(
            result.user()
        );
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        String refreshToken =
            cookieService
                .readRefreshToken(
                    servletRequest
                );

        SessionService.IssuedSession
            issuedSession =
                sessionService.refresh(
                    refreshToken,
                    servletRequest
                        .getRemoteAddr(),
                    servletRequest
                        .getHeader(
                            "User-Agent"
                        )
                );

        cookieService.writeSession(
            servletResponse,
            issuedSession
        );

        SessionService.AuthenticatedSession
            authenticated =
                sessionService
                    .authenticate(
                        issuedSession
                            .accessToken()
                    )
                    .orElseThrow();

        return responseFor(
            authenticated.user()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        sessionService.logout(
            cookieService
                .readAccessToken(
                    request
                ),
            cookieService
                .readRefreshToken(
                    request
                )
        );

        cookieService.clearSession(
            response
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    @GetMapping("/me")
    public AuthResponse me(
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        AppUser user =
            authService.requireUser(
                principal.userId()
            );

        return responseFor(user);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void>
        forgotPassword(
            @Valid
            @RequestBody
            AuthDtos.ForgotPasswordRequest
                request
        ) {

        authService
            .requestPasswordReset(
                request
            );

        return ResponseEntity
            .noContent()
            .build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void>
        resetPassword(
            @Valid
            @RequestBody
            AuthDtos.ResetPasswordRequest
                request,
            HttpServletResponse response
        ) {

        authService.resetPassword(
            request
        );

        cookieService.clearSession(
            response
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void>
        verifyEmail(
            @Valid
            @RequestBody
            AuthDtos.VerifyEmailRequest
                request
        ) {

        authService.verifyEmail(
            request
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void>
        resendVerification(
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        authService
            .resendVerification(
                principal.userId()
            );

        return ResponseEntity
            .noContent()
            .build();
    }

    @PostMapping("/change-password")
    public AuthResponse changePassword(
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        AuthDtos.ChangePasswordRequest
            request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        AuthService.AuthResult result =
            authService.changePassword(
                principal.userId(),
                request,
                servletRequest
                    .getRemoteAddr(),
                servletRequest
                    .getHeader(
                        "User-Agent"
                    )
            );

        cookieService.writeSession(
            servletResponse,
            result.session()
        );

        return responseFor(
            result.user()
        );
    }

    @GetMapping("/sessions")
    public List<
        AuthDtos.SessionView
    > sessions(
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return sessionService
            .listSessions(
                principal.userId()
            )
            .stream()
            .map(
                session ->
                    AuthDtos.SessionView
                        .from(
                            session,
                            principal
                                .sessionId()
                        )
            )
            .toList();
    }

    @DeleteMapping(
        "/sessions/{sessionId}"
    )
    public ResponseEntity<Void>
        revokeSession(
            @PathVariable
            UUID sessionId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            HttpServletResponse response
        ) {

        sessionService.revokeSession(
            principal.userId(),
            sessionId
        );

        if (
            sessionId.equals(
                principal.sessionId()
            )
        ) {
            cookieService.clearSession(
                response
            );
        }

        return ResponseEntity
            .noContent()
            .build();
    }

    private AuthResponse responseFor(
        AppUser user
    ) {
        List<
            OrganizationDtos
                .OrganizationSummary
        > organizations =
            organizationService
                .listForUser(
                    user.getId()
                );

        return new AuthResponse(
            AuthDtos.UserView.from(
                user
            ),
            organizations
        );
    }

    public record AuthResponse(
        AuthDtos.UserView user,
        List<
            OrganizationDtos
                .OrganizationSummary
        > organizations
    ) {
    }
}
