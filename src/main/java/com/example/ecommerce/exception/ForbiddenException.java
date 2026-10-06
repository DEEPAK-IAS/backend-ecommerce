package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 403: authenticated, but not allowed. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
