package com.buildflow.security;

import com.buildflow.auth.service.AuthCookieService;
import com.buildflow.auth.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class OpaqueSessionAuthenticationFilter
    extends OncePerRequestFilter {

    private final AuthCookieService
        cookieService;

    private final SessionService
        sessionService;

    public OpaqueSessionAuthenticationFilter(
        AuthCookieService cookieService,
        SessionService sessionService
    ) {
        this.cookieService =
            cookieService;
        this.sessionService =
            sessionService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (
            SecurityContextHolder
                .getContext()
                .getAuthentication()
                == null
        ) {
            String token =
                cookieService
                    .readAccessToken(
                        request
                    );

            sessionService
                .authenticate(token)
                .ifPresent(
                    authenticated -> {
                        BuildFlowPrincipal principal =
                            new BuildFlowPrincipal(
                                authenticated
                                    .user()
                                    .getId(),
                                authenticated
                                    .sessionId(),
                                authenticated
                                    .user()
                                    .getEmail()
                            );

                        UsernamePasswordAuthenticationToken
                            authentication =
                                new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(
                                        new SimpleGrantedAuthority(
                                            "AUTHENTICATED"
                                        )
                                    )
                                );

                        SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                authentication
                            );
                    }
                );
        }

        filterChain.doFilter(
            request,
            response
        );
    }
}
