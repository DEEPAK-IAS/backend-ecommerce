package com.example.ecommerce.security.jwt;

import com.example.ecommerce.entity.Role;
import java.util.UUID;

public record AccessTokenClaims(UUID userId, Role role) {
}
