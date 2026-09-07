package com.buildflow.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAuthenticationEntryPoint
    implements AuthenticationEntryPoint {

    private final SecurityJsonWriter writer;

    public JsonAuthenticationEntryPoint(
        SecurityJsonWriter writer
    ) {
        this.writer = writer;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {

        writer.write(
            request,
            response,
            HttpServletResponse
                .SC_UNAUTHORIZED,
            "AUTHENTICATION_REQUIRED",
            "Authentication is required."
        );
    }
}
