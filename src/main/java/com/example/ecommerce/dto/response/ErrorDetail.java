package com.example.ecommerce.dto.response;

/** One validation or business error. {@code field} is null for errors not tied to a field. */
public record ErrorDetail(String field, String message) {
}
