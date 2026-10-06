package com.example.ecommerce.dto.response;

import java.time.Instant;

public record PingResponse(String status, Instant serverTime) {
}
