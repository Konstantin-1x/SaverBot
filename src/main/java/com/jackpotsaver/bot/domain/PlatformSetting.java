package com.jackpotsaver.bot.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "platform_settings")
public class PlatformSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private Platform platform;
    @Column(name = "is_enabled")
    private boolean enabled;
    private int maxFileSizeMb;
    private Instant createdAt;
    private Instant updatedAt;

    protected PlatformSetting() {
    }

    public Platform getPlatform() {
        return platform;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setEnabled(boolean enabled, Instant now) {
        this.enabled = enabled;
        this.updatedAt = now;
    }
}
