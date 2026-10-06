package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 409: the request conflicts with current state (e.g. duplicate email). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
