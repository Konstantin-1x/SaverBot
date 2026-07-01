package com.jackpotsaver.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TelegramConnectivityStateTest {
    @Test
    void becomesDownOnlyAfterFailureWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-01T00:00:00Z"));
        TelegramConnectivityState state = new TelegramConnectivityState(clock, 300);

        state.failure();
        assertThat(state.health().getStatus().getCode()).isEqualTo("UP");

        clock.advanceSeconds(301);
        assertThat(state.health().getStatus().getCode()).isEqualTo("DOWN");

        state.success();
        assertThat(state.health().getStatus().getCode()).isEqualTo("UP");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
