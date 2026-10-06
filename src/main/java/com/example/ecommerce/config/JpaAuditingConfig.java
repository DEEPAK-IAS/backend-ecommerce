package com.example.ecommerce.config;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class JpaAuditingConfig {

    /** Always supply UTC instants; avoids implicit JVM-zone conversions. */
    @Bean
    DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
