package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
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
    @ManyToOne(fetch = FetchType.LAZY)
    private User updatedByUser;
    private Instant updatedAt;

    protected AdSettings() {
    }

    public String getAfterDownloadText() {
        return afterDownloadText;
    }

    public void setAfterDownloadText(String afterDownloadText, User updatedByUser, Instant updatedAt) {
        this.afterDownloadText = afterDownloadText;
        this.updatedByUser = updatedByUser;
        this.updatedAt = updatedAt;
    }
}
