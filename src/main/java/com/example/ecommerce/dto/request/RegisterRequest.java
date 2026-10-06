package com.example.ecommerce.dto.request;

import com.example.ecommerce.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @ValidPassword String password,
        @NotBlank String confirmPassword,
        @NotBlank
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Phone must be 8-15 digits with an optional leading +, e.g. +919876543210")
        String phone) {
}
