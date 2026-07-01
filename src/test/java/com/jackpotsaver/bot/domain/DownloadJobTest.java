package com.jackpotsaver.bot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DownloadJobTest {
    private final Instant now = Instant.parse("2026-06-30T00:00:00Z");

    @Test
    void retryReleasesLeaseAndSchedulesNextAttempt() {
        DownloadJob job = new DownloadJob(mock(DownloadRequest.class));
        job.start("worker-1", now);
        Instant retryAt = now.plusSeconds(15);

        job.retry("DOWNLOAD_FAILED", "temporary failure", retryAt);

        assertThat(job.getStatus()).isEqualTo(JobStatus.CREATED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getHeartbeatAt()).isNull();
        assertThat(job.getNextAttemptAt()).isEqualTo(retryAt);
    }

    @Test
    void deadLetterIsTerminalAndReleasesLease() {
        DownloadJob job = new DownloadJob(mock(DownloadRequest.class));
        job.start("worker-1", now);

        job.deadLetter("DOWNLOAD_FAILED", "attempts exhausted", now.plusSeconds(30));

        assertThat(job.getStatus()).isEqualTo(JobStatus.DEAD_LETTER);
        assertThat(job.getHeartbeatAt()).isNull();
        assertThat(job.getAttemptCount()).isEqualTo(1);
    }
}
