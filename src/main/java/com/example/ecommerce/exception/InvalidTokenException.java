package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 401: refresh/reset/verification token is invalid, expired or already used. */
public class InvalidTokenException extends ApiException {

    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
