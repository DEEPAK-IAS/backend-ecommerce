package com.example.ecommerce.dto.response;

/** Returned by login and refresh. {@code expiresIn} is the access-token lifetime in seconds. */
public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
