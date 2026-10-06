package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 409: requested quantity exceeds available stock. */
public class InsufficientStockException extends ApiException {

    public InsufficientStockException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
