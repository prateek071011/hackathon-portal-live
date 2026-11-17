package com.htf.portal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.htf.portal.dto.ApiResponse;
import com.htf.portal.dto.ErrorDetails;

/**
 * Catches every exception thrown by a controller and turns it into the same JSON shape:
 * { success: false, message, data: { status, code, message } }.
 * Expected problems (our PortalExceptions) are logged as WARN; anything unexpected as ERROR with the stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PortalException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handlePortalException(PortalException ex) {
        log.warn("Rule violation [{}]: {}", ex.getCode(), ex.getMessage());
        return build(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    // @Valid failed on a request body: report the first broken field in plain words.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleValidation(MethodArgumentNotValidException ex) {
        String message = "Invalid input";
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null) {
            message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
        }
        log.warn("Validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    // Broken JSON, a missing ?param or upload part, a non-multipart upload,
    // or text where a number was expected (e.g. /api/teams/abc).
    @ExceptionHandler({ HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MultipartException.class,
            MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ApiResponse<ErrorDetails>> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request is missing data or is not in the expected format");
    }

    // Spring rejects uploads above spring.servlet.multipart.max-file-size before our code runs.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return build(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "The file is too large");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleNoEndpoint(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such endpoint: /" + ex.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleWrongMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage());
    }

    // Last safety net. The real cause goes to the log; the client only gets a generic message.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorDetails>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong. Please try again.");
    }

    private ResponseEntity<ApiResponse<ErrorDetails>> build(HttpStatus status, String code, String message) {
        ApiResponse<ErrorDetails> body = ApiResponse.error(status.value(), code, message);
        return ResponseEntity.status(status).body(body);
    }
}
