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
@Table(name = "error_logs")
public class ErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    private DownloadRequest request;
    private String errorCode;
    private String errorMessage;
    private String stackTrace;
    private Instant createdAt;

    protected ErrorLog() {
    }

    public ErrorLog(User user, DownloadRequest request, String errorCode, String errorMessage, String stackTrace, Instant createdAt) {
        this.user = user;
        this.request = request;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.createdAt = createdAt;
    }
}
