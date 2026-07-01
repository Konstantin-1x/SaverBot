package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.Platform;
import com.jackpotsaver.bot.repository.PlatformSettingRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {
    private final PlatformSettingRepository repository;
    private final Clock clock;

    public PlatformService(PlatformSettingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public boolean enabled(Platform platform) {
        return repository.findByPlatform(platform).map(setting -> setting.isEnabled()).orElse(false);
    }

    @Transactional
    public boolean setEnabled(Platform platform, boolean enabled) {
        return repository.findByPlatform(platform)
                .map(setting -> {
                    setting.setEnabled(enabled, clock.instant());
                    return true;
                })
                .orElse(false);
    }
}
