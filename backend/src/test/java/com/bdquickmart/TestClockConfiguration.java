package com.bdquickmart;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the production {@link Clock} with a mutable, primary clock so token
 * expiry can be tested deterministically (no {@code Thread.sleep}).
 */
@TestConfiguration
public class TestClockConfiguration {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    }

    /** A {@link Clock} whose instant can be advanced explicitly. */
    public static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        public MutableClock(Instant instant) {
            this.instant = instant;
            this.zone = ZoneOffset.UTC;
        }

        public void advance(Duration amount) {
            this.instant = this.instant.plus(amount);
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (this.zone.equals(zone)) {
                return this;
            }
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
