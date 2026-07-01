package com.jackpotsaver.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TelegramUpdateTrackerTest {
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TelegramUpdateTracker tracker = new TelegramUpdateTracker(
            jdbcTemplate,
            Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void onlyDatabaseWinnerClaimsUpdate() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1, 0);

        assertThat(tracker.claim(42L)).isTrue();
        assertThat(tracker.claim(42L)).isFalse();
    }
}
