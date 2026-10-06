package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 401: missing or invalid credentials. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
