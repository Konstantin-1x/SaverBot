package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.AdSettings;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.repository.AdSettingsRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdService {
    private static final long SETTINGS_ID = 1L;
    private final AdSettingsRepository repository;
    private final Clock clock;
    private final JdbcTemplate jdbc;

    public AdService(AdSettingsRepository repository, Clock clock, JdbcTemplate jdbc) {
        this.repository = repository;
        this.clock = clock;
        this.jdbc = jdbc;
    }

    @Transactional
    public Optional<MediaContent> contentForCompletedDownload(Long userId) {
        Long completed = jdbc.queryForObject("""
                update users
                set completed_downloads = completed_downloads + 1
                where id = ?
                returning completed_downloads
                """, Long.class, userId);
        return repository.findById(SETTINGS_ID)
                .filter(settings -> completed != null
                        && settings.getFrequency() > 0
                        && completed % settings.getFrequency() == 0)
                .map(settings -> new MediaContent(
                        settings.getMediaType(),
                        settings.getTelegramFileId(),
                        settings.getAfterDownloadText()))
                .filter(content -> !content.empty());
    }

    @Transactional
    public void setAfterDownloadContent(MediaContent content, User admin) {
        AdSettings settings = repository.findById(SETTINGS_ID).orElseThrow();
        settings.setAfterDownloadContent(
                blankToNull(content.text()), content.mediaType(), blankToNull(content.telegramFileId()),
                admin, clock.instant());
    }

    @Transactional
    public void setFrequency(int frequency, User admin) {
        if (frequency < 1 || frequency > 100_000) {
            throw new IllegalArgumentException("Ad frequency must be between 1 and 100000");
        }
        AdSettings settings = repository.findById(SETTINGS_ID).orElseThrow();
        settings.setFrequency(frequency, admin, clock.instant());
    }

    @Transactional
    public void clearAfterDownloadText(User admin) {
        AdSettings settings = repository.findById(SETTINGS_ID).orElseThrow();
        settings.setAfterDownloadContent(null, null, null, admin, clock.instant());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
