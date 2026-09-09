package com.buildflow.security.hardening;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

final class SecurityFilterResponses {

    private SecurityFilterResponses() {
    }

    static void write(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code,
        String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding(
            "UTF-8"
        );
        response.setContentType(
            "application/json"
        );
        response.setHeader(
            "Cache-Control",
            "no-store"
        );

        String traceId =
            request.getHeader(
                "X-Request-Id"
            );

        if (
            traceId == null
            || traceId.isBlank()
        ) {
            traceId = "none";
        }

        response.getWriter()
            .write(
                "{"
                    + "\"code\":\""
                    + escape(code)
                    + "\","
                    + "\"message\":\""
                    + escape(message)
                    + "\","
                    + "\"fieldErrors\":[],"
                    + "\"timestamp\":\""
                    + Instant.now()
                    + "\","
                    + "\"traceId\":\""
                    + escape(traceId)
                    + "\""
                    + "}"
            );

        response.flushBuffer();
    }

    private static String escape(
        String value
    ) {
        return value
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                "\"",
                "\\\""
            )
            .replace(
                "\n",
                "\\n"
            )
            .replace(
                "\r",
                "\\r"
            );
    }
}
