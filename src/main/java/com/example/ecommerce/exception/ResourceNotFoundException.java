package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/** Maps to 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "%s not found with id: %s".formatted(resource, id));
    }
}
