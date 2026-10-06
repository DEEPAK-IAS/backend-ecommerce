package com.example.ecommerce.dto.response;

import com.example.ecommerce.entity.AccountStatus;
import com.example.ecommerce.entity.Role;
import java.time.Instant;
import java.util.UUID;

/** Public view of a user. Deliberately has no password or hash field. */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        Role role,
        AccountStatus status,
        Instant createdAt) {
}
