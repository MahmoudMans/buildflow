package com.buildflow.security;

import com.buildflow.shared.error.ApiError;
import com.buildflow.shared.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class SecurityJsonWriter {

    private final JsonMapper jsonMapper;

    public SecurityJsonWriter(
        JsonMapper jsonMapper
    ) {
        this.jsonMapper = jsonMapper;
    }

    public void write(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code,
        String message
    ) throws IOException {

        response.setStatus(status);

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        Object traceId =
            request.getAttribute(
                TraceIdFilter.REQUEST_ATTRIBUTE
            );

        ApiError error =
            ApiError.create(
                code,
                message,
                List.of(),
                traceId == null
                    ? "unknown"
                    : traceId.toString()
            );

        jsonMapper.writeValue(
            response.getOutputStream(),
            error
        );
    }
}
