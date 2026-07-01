package com.jackpotsaver.bot.telegram;

import java.time.Clock;
import java.time.Duration;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TelegramUpdateTracker {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public TelegramUpdateTracker(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public boolean claim(long updateId) {
        var now = clock.instant();
        var staleBefore = now.minus(Duration.ofMinutes(5));
        return jdbcTemplate.update("""
                insert into telegram_updates(update_id, status, processing_at)
                values (?, 'PROCESSING', ?)
                on conflict (update_id) do update
                set status = 'PROCESSING', processing_at = excluded.processing_at, error_message = null
                where telegram_updates.status = 'FAILED'
                   or (telegram_updates.status = 'PROCESSING' and telegram_updates.processing_at < ?)
                """, updateId, Timestamp.from(now), Timestamp.from(staleBefore)) == 1;
    }

    public void complete(long updateId) {
        jdbcTemplate.update("""
                update telegram_updates
                set status = 'COMPLETED', completed_at = ?, error_message = null
                where update_id = ?
                """, Timestamp.from(clock.instant()), updateId);
    }

    public void fail(long updateId, String error) {
        jdbcTemplate.update("""
                update telegram_updates
                set status = 'FAILED', error_message = ?
                where update_id = ?
                """, abbreviate(error), updateId);
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
