package com.bdquickmart.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

/**
 * Central clock used for JWT timestamps and JPA audit fields.
 *
 * <p>Injecting a single {@link Clock} (instead of calling {@code Instant.now()}
 * directly) lets the integration tests substitute a mutable clock and exercise
 * token expiry deterministically, with no {@code Thread.sleep}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    /** Wires JPA auditing to the injectable clock. */
    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
