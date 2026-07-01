package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.DownloadProperties;
import com.jackpotsaver.bot.domain.DownloadJob;
import com.jackpotsaver.bot.domain.JobStatus;
import com.jackpotsaver.bot.repository.DownloadJobRepository;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DownloadWorker {
    private static final Logger log = LoggerFactory.getLogger(DownloadWorker.class);
    private final DownloadJobRepository jobRepository;
    private final DownloadProperties properties;
    private final DownloadJobProcessor processor;
    private final Clock clock;
    private final String workerId = UUID.randomUUID().toString();
    private final ExecutorService executor;
    private final Set<Long> runningJobIds = ConcurrentHashMap.newKeySet();
    private final int maxParallelJobs;

    public DownloadWorker(DownloadJobRepository jobRepository, DownloadProperties properties,
                          DownloadJobProcessor processor, Clock clock) {
        this.jobRepository = jobRepository;
        this.properties = properties;
        this.processor = processor;
        this.clock = clock;
        this.maxParallelJobs = Math.max(1, properties.maxParallelJobs());
        this.executor = new ThreadPoolExecutor(
                maxParallelJobs,
                maxParallelJobs,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxParallelJobs),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Scheduled(fixedDelayString = "${download.worker-interval-ms:5000}")
    public void run() {
        heartbeatRunningJobs();
        requeueStaleJobs();
        int availableSlots = maxParallelJobs - runningJobIds.size();
        if (availableSlots <= 0) {
            return;
        }
        List<Long> jobIds = jobRepository.findReadyJobs(
                        JobStatus.CREATED, clock.instant(), PageRequest.of(0, 10)).stream()
                .map(DownloadJob::getId)
                .filter(jobId -> !runningJobIds.contains(jobId))
                .limit(availableSlots)
                .toList();
        jobIds.stream()
                .filter(runningJobIds::add)
                .forEach(this::submit);
    }

    private void submit(Long jobId) {
        try {
            executor.submit(() -> {
                try {
                    processor.process(jobId, workerId);
                } finally {
                    runningJobIds.remove(jobId);
                }
            });
        } catch (RejectedExecutionException ex) {
            runningJobIds.remove(jobId);
            if (!executor.isShutdown()) {
                log.warn("Download executor rejected job {}", jobId, ex);
            }
        }
    }

    private void heartbeatRunningJobs() {
        if (!runningJobIds.isEmpty()) {
            jobRepository.heartbeatWorkerJobs(workerId, clock.instant(), JobStatus.RUNNING);
        }
    }

    private void requeueStaleJobs() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(Duration.ofMinutes(properties.stalledJobTimeoutMinutes()));
        int deadLettered = jobRepository.deadLetterStaleRunningJobs(
                cutoff, now, properties.maxAttempts(), JobStatus.RUNNING, JobStatus.DEAD_LETTER);
        int requeued = jobRepository.requeueStaleRunningJobs(
                cutoff,
                now.plusSeconds(properties.retryDelaySeconds()),
                properties.maxAttempts(),
                JobStatus.RUNNING,
                JobStatus.CREATED
        );
        if (requeued > 0) {
            log.warn("Requeued {} stale RUNNING download jobs older than {}", requeued, cutoff);
        }
        if (deadLettered > 0) {
            log.error("Moved {} stale download jobs to DEAD_LETTER after {} attempts",
                    deadLettered, properties.maxAttempts());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Download worker did not stop within 30 seconds; forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
