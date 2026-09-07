package com.buildflow.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler
    implements AccessDeniedHandler {

    private final SecurityJsonWriter writer;

    public JsonAccessDeniedHandler(
        SecurityJsonWriter writer
    ) {
        this.writer = writer;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException, ServletException {

        writer.write(
            request,
            response,
            HttpServletResponse
                .SC_FORBIDDEN,
            "ACCESS_DENIED",
            "You do not have permission to perform this operation."
        );
    }
}
