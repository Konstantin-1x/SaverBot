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
@Table(name = "download_requests")
public class DownloadRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    private String sourceUrl;
    private String normalizedUrl;
    private String resourceKey;
    @Enumerated(EnumType.STRING)
    private Platform platform;
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;
    @Enumerated(EnumType.STRING)
    private VideoQuality selectedQuality;
    @Enumerated(EnumType.STRING)
    private RequestStatus status;
    private Long telegramChatId;
    private Integer loadingMessageId;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    protected DownloadRequest() {
    }

    public DownloadRequest(User user, String sourceUrl, String normalizedUrl, Platform platform, MediaType mediaType,
                           VideoQuality selectedQuality, RequestStatus status, Instant now) {
        this.user = user;
        this.sourceUrl = sourceUrl;
        this.normalizedUrl = normalizedUrl;
        this.platform = platform;
        this.mediaType = mediaType;
        this.selectedQuality = selectedQuality;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public Platform getPlatform() {
        return platform;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public VideoQuality getSelectedQuality() {
        return selectedQuality;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public Integer getLoadingMessageId() {
        return loadingMessageId;
    }

    public void setSelectedQuality(VideoQuality selectedQuality, Instant now) {
        this.selectedQuality = selectedQuality;
        touch(now);
    }

    public void setResourceKey(String resourceKey, Instant now) {
        this.resourceKey = resourceKey;
        touch(now);
    }

    public void attachTelegramLoading(long chatId, Integer loadingMessageId, Instant now) {
        this.telegramChatId = chatId;
        this.loadingMessageId = loadingMessageId;
        touch(now);
    }

    public void status(RequestStatus status, Instant now) {
        this.status = status;
        if (status == RequestStatus.SUCCESS || status == RequestStatus.FAILED || status == RequestStatus.CACHED) {
            this.completedAt = now;
        }
        touch(now);
    }

    public void fail(String errorMessage, Instant now) {
        this.errorMessage = errorMessage;
        status(RequestStatus.FAILED, now);
    }

    public void redactUrls() {
        this.sourceUrl = null;
        this.normalizedUrl = null;
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }
}
