package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "download_job_subscribers")
public class DownloadJobSubscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DownloadJob job;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DownloadRequest request;
    private Instant createdAt;

    protected DownloadJobSubscriber() {
    }

    public DownloadJobSubscriber(DownloadJob job, DownloadRequest request, Instant createdAt) {
        this.job = job;
        this.request = request;
        this.createdAt = createdAt;
    }

    public DownloadRequest getRequest() {
        return request;
    }
}
