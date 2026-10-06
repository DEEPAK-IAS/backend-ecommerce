package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 400: the request is syntactically valid but violates a business rule. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
