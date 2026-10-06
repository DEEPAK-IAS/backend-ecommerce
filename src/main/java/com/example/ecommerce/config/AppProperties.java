package com.example.ecommerce.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed access to the {@code app.*} section of application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors) {

    public record Cors(List<String> allowedOrigins) {
    }
}
