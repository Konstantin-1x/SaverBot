package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ad_settings")
public class AdSettings {
    @Id
    private Long id;
    private String afterDownloadText;
    @Enumerated(EnumType.STRING)
    private AdMediaType mediaType;
    private String telegramFileId;
    private int frequency;
    @ManyToOne(fetch = FetchType.LAZY)
    private User updatedByUser;
    private Instant updatedAt;

    protected AdSettings() {
    }

    public String getAfterDownloadText() {
        return afterDownloadText;
    }

    public AdMediaType getMediaType() {
        return mediaType;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setAfterDownloadContent(String afterDownloadText, AdMediaType mediaType, String telegramFileId,
                                        User updatedByUser, Instant updatedAt) {
        this.afterDownloadText = afterDownloadText;
        this.mediaType = mediaType;
        this.telegramFileId = telegramFileId;
        this.updatedByUser = updatedByUser;
        this.updatedAt = updatedAt;
    }

    public void setFrequency(int frequency, User updatedByUser, Instant updatedAt) {
        this.frequency = frequency;
        this.updatedByUser = updatedByUser;
        this.updatedAt = updatedAt;
    }
}
