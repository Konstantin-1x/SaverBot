package com.jackpotsaver.bot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jackpotsaver.bot.domain.DownloadJob;
import com.jackpotsaver.bot.domain.DownloadRequest;
import com.jackpotsaver.bot.domain.InterfaceLanguage;
import com.jackpotsaver.bot.domain.JobStatus;
import com.jackpotsaver.bot.domain.MediaType;
import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.domain.RequestStatus;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.domain.VideoQuality;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import com.jackpotsaver.bot.repository.DownloadRequestRepository;
import com.jackpotsaver.bot.repository.UserRepository;
import com.jackpotsaver.bot.telegram.TelegramUpdateTracker;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostgresIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    UserRepository users;
    @Autowired
    DownloadRequestRepository requests;
    @Autowired
    DownloadJobRepository jobs;

    @Test
    void flywayCreatesLatestSchema() {
        Integer version = jdbcTemplate.queryForObject(
                "select max(version::integer) from flyway_schema_history where success", Integer.class);

        assertThat(version).isEqualTo(12);
        assertThat(tableExists("telegram_updates")).isTrue();
        assertThat(tableExists("download_job_subscribers")).isTrue();
        assertThat(columnExists("stored_files", "normalized_url")).isFalse();
        assertThat(columnExists("download_requests", "resource_key")).isTrue();
        assertThat(columnNullable("download_requests", "source_url")).isTrue();
    }

    @Test
    void onlyOneWorkerAtomicallyClaimsJob() throws Exception {
        DownloadJob job = jobs.saveAndFlush(new DownloadJob(request(), "claim-test"));
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> first = () -> jobs.claimCreatedJob(
                    job.getId(), "worker-1", now, JobStatus.CREATED, JobStatus.RUNNING);
            Callable<Integer> second = () -> jobs.claimCreatedJob(
                    job.getId(), "worker-2", now, JobStatus.CREATED, JobStatus.RUNNING);

            int claimed = executor.invokeAll(List.of(first, second)).stream()
                    .mapToInt(future -> {
                        try {
                            return future.get();
                        } catch (Exception ex) {
                            throw new AssertionError(ex);
                        }
                    }).sum();

            assertThat(claimed).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void onlyOneConcurrentTelegramUpdateClaimWins() throws Exception {
        TelegramUpdateTracker tracker = new TelegramUpdateTracker(
                jdbcTemplate, Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC));
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> claims = List.of(() -> tracker.claim(777L), () -> tracker.claim(777L));
            var results = executor.invokeAll(claims);
            long winners = results.stream().filter(result -> {
                try {
                    return result.get();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }).count();

            assertThat(winners).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void activeDeduplicationIndexRejectsSecondLeader() {
        jobs.saveAndFlush(new DownloadJob(request(), "same-download"));

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> jobs.saveAndFlush(new DownloadJob(request(), "same-download")));
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select to_regclass('public.' || ?) is not null", Boolean.class, table);
        return Boolean.TRUE.equals(exists);
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = ?
                      and column_name = ?
                )
                """, Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private boolean columnNullable(String table, String column) {
        String nullable = jdbcTemplate.queryForObject("""
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """, String.class, table, column);
        return "YES".equals(nullable);
    }

    private DownloadRequest request() {
        Instant now = Instant.now();
        User user = users.save(new User(
                System.nanoTime(), "integration", "Test", "User", "en", now, false));
        user.setInterfaceLanguage(InterfaceLanguage.EN, now);
        return requests.save(new DownloadRequest(
                user, "https://youtu.be/test", "https://youtube.com/watch?v=test",
                Platform.YOUTUBE, MediaType.YOUTUBE_VIDEO, VideoQuality.LOW,
                RequestStatus.LOADING, now));
    }
}
