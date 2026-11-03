package com.example.app.common.exception;

import com.example.app.common.response.ApiError;
import com.example.app.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {} - {}", ex.getCode(), ex.getMessage());
        ApiError error = ApiError.of(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(resolveHttpStatus(ex.getCode()))
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());

        log.warn("Validation failed: {}", validationErrors);
        ApiError error = ApiError.of("VALIDATION_ERROR", "Request validation failed", validationErrors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex, HttpServletRequest request) {
        List<ApiError.ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());

        ApiError error = ApiError.of("VALIDATION_ERROR", "Request binding failed", validationErrors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.ValidationError> validationErrors = ex.getConstraintViolations().stream()
            .map(this::mapConstraintViolation)
            .collect(Collectors.toList());

        ApiError error = ApiError.of("VALIDATION_ERROR", "Constraint validation failed", validationErrors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ApiError error = ApiError.of("MISSING_PARAMETER", String.format("Required parameter '%s' is missing", ex.getParameterName()));
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ApiError error = ApiError.of("TYPE_MISMATCH", String.format("Parameter '%s' should be of type %s", ex.getName(), ex.getRequiredType().getSimpleName()));
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ApiError error = ApiError.of("MALFORMED_REQUEST", "Request body is malformed or missing");
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        ApiError error = ApiError.of("ENDPOINT_NOT_FOUND", String.format("Endpoint %s %s not found", ex.getHttpMethod(), ex.getRequestURL()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ApiError error = ApiError.of("UNAUTHORIZED", "Invalid or missing authentication");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ApiError error = ApiError.of("INVALID_CREDENTIALS", "Invalid username or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiError error = ApiError.of("FORBIDDEN", "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation", ex);
        String message = ex.getMostSpecificCause().getMessage();
        String errorCode = "DATA_INTEGRITY_VIOLATION";
        String errorMessage = "Data integrity violation";

        if (message != null && message.contains("duplicate key")) {
            errorCode = "DUPLICATE_RESOURCE";
            errorMessage = "Resource already exists";
        } else if (message != null && message.contains("foreign key")) {
            errorCode = "FOREIGN_KEY_VIOLATION";
            errorMessage = "Referenced resource does not exist";
        }

        ApiError error = ApiError.of(errorCode, errorMessage);
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        ApiError error = ApiError.of("INTERNAL_SERVER_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error, getRequestId(request)));
    }

    private ApiError.ValidationError mapFieldError(FieldError fieldError) {
        return ApiError.ValidationError.builder()
            .field(fieldError.getField())
            .message(fieldError.getDefaultMessage())
            .rejectedValue(fieldError.getRejectedValue())
            .build();
    }

    private ApiError.ValidationError mapConstraintViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        return ApiError.ValidationError.builder()
            .field(field)
            .message(violation.getMessage())
            .rejectedValue(violation.getInvalidValue())
            .build();
    }

    private HttpStatus resolveHttpStatus(String code) {
        return switch (code) {
            case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_RESOURCE", "DATA_INTEGRITY_VIOLATION" -> HttpStatus.CONFLICT;
            case "UNAUTHORIZED", "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "VALIDATION_ERROR", "MISSING_PARAMETER", "TYPE_MISMATCH", "MALFORMED_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "ENDPOINT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

}