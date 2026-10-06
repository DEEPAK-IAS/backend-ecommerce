package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all expected (business) errors. The HTTP status lives on the exception,
 * so GlobalExceptionHandler needs only one handler for all of them.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
