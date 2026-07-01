package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "download_jobs")
public class DownloadJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DownloadRequest request;
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private int attemptCount;
    private int priority;
    private Instant startedAt;
    private Instant heartbeatAt;
    private Instant nextAttemptAt;
    private Instant finishedAt;
    private String workerId;
    private String errorCode;
    private String errorDetails;
    private String dedupKey;

    protected DownloadJob() {
    }

    public DownloadJob(DownloadRequest request) {
        this(request, null);
    }

    public DownloadJob(DownloadRequest request, String dedupKey) {
        this.request = request;
        this.dedupKey = dedupKey;
        this.status = JobStatus.CREATED;
        this.priority = 100;
    }

    public Long getId() {
        return id;
    }

    public DownloadRequest getRequest() {
        return request;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Instant getHeartbeatAt() {
        return heartbeatAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void start(String workerId, Instant now) {
        this.status = JobStatus.RUNNING;
        this.workerId = workerId;
        this.startedAt = now;
        this.heartbeatAt = now;
        this.nextAttemptAt = null;
        this.attemptCount++;
    }

    public void finish(Instant now) {
        this.status = JobStatus.SUCCESS;
        this.workerId = null;
        this.heartbeatAt = null;
        this.finishedAt = now;
    }

    public void retry(String code, String details, Instant nextAttemptAt) {
        this.status = JobStatus.CREATED;
        this.workerId = null;
        this.startedAt = null;
        this.heartbeatAt = null;
        this.nextAttemptAt = nextAttemptAt;
        this.errorCode = code;
        this.errorDetails = details;
    }

    public void deadLetter(String code, String details, Instant now) {
        this.status = JobStatus.DEAD_LETTER;
        this.workerId = null;
        this.heartbeatAt = null;
        this.errorCode = code;
        this.errorDetails = details;
        this.finishedAt = now;
    }

    public void fail(String code, String details, Instant now) {
        this.status = JobStatus.FAILED;
        this.workerId = null;
        this.heartbeatAt = null;
        this.errorCode = code;
        this.errorDetails = details;
        this.finishedAt = now;
    }
}
