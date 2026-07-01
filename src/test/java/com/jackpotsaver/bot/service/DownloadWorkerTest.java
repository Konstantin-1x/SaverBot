package com.jackpotsaver.bot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jackpotsaver.bot.config.DownloadProperties;
import com.jackpotsaver.bot.domain.DownloadJob;
import com.jackpotsaver.bot.domain.JobStatus;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class DownloadWorkerTest {
    private final DownloadJobRepository repository = mock(DownloadJobRepository.class);
    private final DownloadJobProcessor processor = mock(DownloadJobProcessor.class);
    private DownloadWorker worker;

    @AfterEach
    void shutdown() {
        if (worker != null) {
            worker.shutdown();
        }
    }

    @Test
    void doesNotQueueMoreJobsWhileAllWorkerSlotsAreBusy() throws Exception {
        DownloadProperties properties = new DownloadProperties(
                Path.of("tmp"), Path.of("storage"), 2, 48, 24, 30, 1000, 60, 3, 15, 180, "yt-dlp");
        worker = new DownloadWorker(repository, properties, processor,
                Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC));

        List<DownloadJob> jobs = List.of(job(1L), job(2L), job(3L), job(4L));
        when(repository.findReadyJobs(eq(JobStatus.CREATED), any(), any(Pageable.class))).thenReturn(jobs);

        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(processor).process(any(), anyString());

        worker.run();
        started.await(2, TimeUnit.SECONDS);
        worker.run();
        worker.run();

        verify(processor, timeout(1000).times(2)).process(any(), anyString());
        verify(repository, atLeastOnce()).heartbeatWorkerJobs(anyString(), any(), eq(JobStatus.RUNNING));
        release.countDown();
    }

    @Test
    void takesNextJobAfterCapacityBecomesAvailable() throws Exception {
        DownloadProperties properties = new DownloadProperties(
                Path.of("tmp"), Path.of("storage"), 1, 48, 24, 30, 1000, 60, 3, 15, 180, "yt-dlp");
        worker = new DownloadWorker(repository, properties, processor,
                Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC));

        DownloadJob first = job(1L);
        DownloadJob second = job(2L);
        when(repository.findReadyJobs(eq(JobStatus.CREATED), any(), any(Pageable.class)))
                .thenReturn(List.of(first), List.of(second));

        worker.run();
        verify(processor, timeout(1000)).process(eq(1L), anyString());
        worker.run();

        verify(processor, timeout(1000)).process(eq(2L), anyString());
    }

    private DownloadJob job(long id) {
        DownloadJob job = mock(DownloadJob.class);
        when(job.getId()).thenReturn(id);
        return job;
    }
}
