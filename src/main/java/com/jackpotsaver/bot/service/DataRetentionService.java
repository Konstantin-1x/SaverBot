package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.RetentionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataRetentionService {
    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);
    private final JdbcTemplate jdbc;
    private final RetentionProperties properties;
    private final Clock clock;
    private final Counter deletedRows;

    public DataRetentionService(JdbcTemplate jdbc, RetentionProperties properties, Clock clock,
                                MeterRegistry registry) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.clock = clock;
        this.deletedRows = Counter.builder("jackpot_retention_deleted_rows_total")
                .description("Rows removed by retention cleanup")
                .register(registry);
    }

    @Scheduled(cron = "${retention.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void cleanup() {
        int deleted = 0;
        deleted += jdbc.update("delete from telegram_updates where completed_at < ?",
                before(properties.telegramUpdatesDays()));
        deleted += jdbc.update("delete from error_logs where created_at < ?",
                before(properties.errorLogsDays()));
        deleted += jdbc.update("delete from admin_actions where created_at < ?",
                before(properties.adminActionsDays()));
        deleted += jdbc.update("""
                delete from stored_files
                where deleted_at < ?
                  and status in ('DELETED', 'EXPIRED')
                """, before(properties.deletedFilesDays()));
        deleted += jdbc.update("""
                delete from download_jobs
                where finished_at < ?
                  and status in ('SUCCESS', 'FAILED', 'DEAD_LETTER')
                  and not exists (
                    select 1 from stored_files file where file.download_request_id = download_jobs.request_id
                  )
                """, before(properties.requestsDays()));
        deleted += jdbc.update("""
                delete from download_requests request
                where request.completed_at < ?
                  and request.status in ('SUCCESS', 'FAILED', 'CACHED')
                  and not exists (select 1 from download_jobs job where job.request_id = request.id)
                  and not exists (select 1 from stored_files file where file.download_request_id = request.id)
                """, before(properties.requestsDays()));
        if (deleted > 0) {
            deletedRows.increment(deleted);
            log.info("Retention cleanup removed {} database rows", deleted);
        }
    }

    private Timestamp before(int days) {
        return Timestamp.from(clock.instant().minus(Duration.ofDays(days)));
    }
}

