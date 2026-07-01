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
@Table(name = "stored_files")
public class StoredFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DownloadRequest downloadRequest;
    private String filePath;
    private long fileSize;
    private String telegramFileId;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant deletedAt;
    @Enumerated(EnumType.STRING)
    private FileStatus status;

    protected StoredFile() {
    }

    public StoredFile(DownloadRequest downloadRequest, String filePath, long fileSize,
                      Instant createdAt, Instant expiresAt) {
        this.downloadRequest = downloadRequest;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = FileStatus.STAGING;
    }

    public Long getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public Platform getPlatform() {
        return downloadRequest.getPlatform();
    }

    public MediaType getMediaType() {
        return downloadRequest.getMediaType();
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void markAvailable() {
        this.status = FileStatus.AVAILABLE;
    }

    public void setTelegramFileId(String telegramFileId) {
        this.telegramFileId = telegramFileId;
    }

    public void markDeleted(Instant now) {
        this.status = FileStatus.DELETED;
        this.deletedAt = now;
    }

    public void markExpired(Instant now) {
        this.status = FileStatus.EXPIRED;
        this.deletedAt = now;
    }
}
