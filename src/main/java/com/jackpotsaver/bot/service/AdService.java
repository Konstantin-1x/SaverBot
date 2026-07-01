package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.AdSettings;
import com.jackpotsaver.bot.domain.User;
import com.jackpotsaver.bot.repository.AdSettingsRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdService {
    private static final long SETTINGS_ID = 1L;
    private final AdSettingsRepository repository;
    private final Clock clock;

    public AdService(AdSettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<String> afterDownloadText() {
        return repository.findById(SETTINGS_ID)
                .map(AdSettings::getAfterDownloadText)
                .filter(text -> text != null && !text.isBlank());
    }

    @Transactional
    public void setAfterDownloadText(String text, User admin) {
        AdSettings settings = repository.findById(SETTINGS_ID).orElseThrow();
        settings.setAfterDownloadText(text, admin, clock.instant());
    }

    @Transactional
    public void clearAfterDownloadText(User admin) {
        AdSettings settings = repository.findById(SETTINGS_ID).orElseThrow();
        settings.setAfterDownloadText(null, admin, clock.instant());
    }
}
