package com.buildflow.shared.error;

import com.buildflow.shared.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
        BusinessException exception,
        HttpServletRequest request
    ) {
        return response(
            exception.getStatus(),
            exception.getCode(),
            exception.getMessage(),
            List.of(),
            request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<FieldViolation> violations =
            exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                    new FieldViolation(
                        error.getField(),
                        Objects.requireNonNullElse(
                            error.getDefaultMessage(),
                            "Invalid value"
                        )
                    )
                )
                .toList();

        return response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "One or more fields are invalid.",
            violations,
            request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<FieldViolation> violations =
            exception.getConstraintViolations()
                .stream()
                .map(violation ->
                    new FieldViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                    )
                )
                .toList();

        return response(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "One or more request values are invalid.",
            violations,
            request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST",
            "The request body could not be read.",
            List.of(),
            request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
        MissingServletRequestParameterException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "MISSING_REQUEST_PARAMETER",
            "A required request parameter is missing.",
            List.of(),
            request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.BAD_REQUEST,
            "INVALID_REQUEST_PARAMETER",
            "A request parameter has an invalid value.",
            List.of(),
            request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "This HTTP method is not supported.",
            List.of(),
            request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(
        NoResourceFoundException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "The requested resource does not exist.",
            List.of(),
            request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
        DataIntegrityViolationException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.CONFLICT,
            "DATA_CONFLICT",
            "The operation conflicts with existing data.",
            List.of(),
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        String traceId = traceId(request);

        log.error(
            "Unhandled request failure traceId={}",
            traceId,
            exception
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiError.create(
                    "INTERNAL_SERVER_ERROR",
                    "An unexpected server error occurred.",
                    List.of(),
                    traceId
                )
            );
    }

    private ResponseEntity<ApiError> response(
        HttpStatus status,
        String code,
        String message,
        List<FieldViolation> fieldErrors,
        HttpServletRequest request
    ) {
        return ResponseEntity
            .status(status)
            .body(
                ApiError.create(
                    code,
                    message,
                    fieldErrors,
                    traceId(request)
                )
            );
    }

    private String traceId(
        HttpServletRequest request
    ) {
        Object traceId =
            request.getAttribute(
                TraceIdFilter.REQUEST_ATTRIBUTE
            );

        if (traceId == null) {
            return "unknown";
        }

        return traceId.toString();
    }
}
